# ResolveX

ResolveX is an incident-management REST API built with Spring Boot, PostgreSQL,
Flyway, Docker, Testcontainers, and OpenAPI. It currently supports incident
creation, retrieval, updates, deletion, filtering, sorting, pagination, and
grouping.

This document is the proposed security architecture for authentication,
authorization, user ownership, and incident assignment. It is a design record,
not a claim that these capabilities are already implemented.

## 1. Decision Summary

ResolveX should initially use the following design:

- Stateless HTTP authentication using short-lived, signed JSON Web Tokens
  (JWTs).
- PostgreSQL as the source of truth for user accounts, roles, incident creators,
  and incident assignees.
- Role-based access control (RBAC) with one fixed role per user: `ADMIN`,
  `AGENT`, or `VIEWER`.
- Object-level authorization for incidents: an AGENT may update only an incident
  assigned to them, while an ADMIN can manage every incident.
- Administrator-only user provisioning for the first release. Public
  registration is not appropriate for an internal incident-management system.
- A nullable incident assignee, because a newly reported incident may be
  unassigned.
- No user-group, team, dynamic-role, permission, refresh-token, or audit-event
  tables in the first release. Their need will be reassessed after the core
  workflow is in use.

This design gives a secure, understandable foundation without prematurely
introducing a complex identity-and-access-management system.

## 2. Architecture

### 2.1 Stateless API architecture

ResolveX will be stateless with respect to login sessions. A successful login
returns an access token. The client sends that token with every protected API
request:

```http
Authorization: Bearer <access-token>
```

The application validates the token on every request and does not keep an HTTP
session in application memory. PostgreSQL still stores durable business data,
including users, password hashes, roles, incidents, and assignment references.
Stateless means only that the server does not store a per-login web session.

```text
Untrusted client                  ResolveX API                   PostgreSQL
       |                                |                              |
       | POST /auth/login               |                              |
       | email + password               |--- find active user -------->|
       |                                |<-- user + password hash ------|
       |                                |    verify password             |
       |<--- short-lived JWT -----------|                              |
       |                                                               |
       | GET /incidents/{id}            |                              |
       | Bearer JWT                     |                              |
       |------------------------------->| validate signature + expiry   |
       |                                | load current user/role ------>|
       |                                | object-level access check     |
       |<--- permitted response --------|                              |
```

This fits a REST API and Docker deployment: any healthy ResolveX application
container can process a request without requiring session affinity or shared
server-memory state.

### 2.2 Authentication versus authorization

Authentication answers **who is making the request?** A user provides an email
and password at login. The API finds the account, compares the supplied password
with the stored password hash, and issues a signed token if they match.

Authorization answers **may this authenticated user perform this action on this
resource?** It has two layers:

1. **Role-level authorization:** Is the user's role permitted to call this type
   of endpoint?
2. **Object-level authorization:** For a specific incident, is the user the
   assignee or an administrator? This prevents an AGENT from changing another
   AGENT's assigned incident merely by guessing its ID.

Both checks are necessary. Role-only authorization is not sufficient once
incidents are assigned to particular users. OWASP identifies missing object-level
authorization as a major API risk; it must be tested for every endpoint that
accepts an incident or user identifier. [OWASP API Security guidance](https://devguide.owasp.org/en/07-training-education/07-api-top-ten/)
and [OWASP's BOLA testing guide](https://wstg.owasp.org/latest/4-Web_Application_Security_Testing/12-API_Testing/02-API_Broken_Object_Level_Authorization/)
cover this risk.

## 3. Assets, Threats, and Trust Boundaries

### 3.1 Assets to protect

| Asset | Why it matters | Protection direction |
| --- | --- | --- |
| Passwords and password hashes | Account takeover if exposed or weakly stored | Slow password hashing, never return or log values |
| JWT signing key | Forged tokens if exposed | Environment secret manager, rotation procedure, never Git |
| JWT access token | Bearer credential; theft allows impersonation until expiry | HTTPS, short expiry, client-side storage policy |
| User accounts and roles | Role changes can grant administrative power | ADMIN-only provisioning and authorization checks |
| Incident content and assignments | May contain sensitive operational information | Authenticated access and object-level checks |
| Database credentials | Database compromise if leaked | `.env` ignored by Git; production secret store |
| Audit and security events | Needed to investigate misuse | Append-only or protected event records in a later phase |

### 3.2 Primary threats and mitigations

| Threat | Example | Primary mitigation |
| --- | --- | --- |
| Credential stuffing or password guessing | Repeated login attempts | Rate limiting, generic failures, strong password policy |
| Password database exposure | A database backup is leaked | BCrypt password hashes and least-privileged DB account |
| Token theft | Token is copied from browser storage or logs | HTTPS, short expiry, do not log tokens, secure client storage |
| Token forgery | Attacker attempts to sign an ADMIN token | Strong signing key and strict signature/algorithm validation |
| Vertical privilege escalation | AGENT calls an ADMIN-only endpoint | Deny-by-default route rules and role tests |
| Horizontal privilege escalation | AGENT updates another AGENT's incident | Assignment-aware object-level authorization |
| User enumeration | Login reveals whether an email exists | Identical generic failure responses |
| Secret exposure | JWT or DB secret is committed to Git | `.env` ignored, `.env.example` contains placeholders only |
| SQL injection | Filter or login input alters a query | JPA parameter binding, DTO validation, no dynamic SQL concatenation |
| Accidental data exposure | API response includes a password hash | Separate response DTOs; never expose entity directly |

NIST recommends throttling failed authentications and secure, salted password
verification storage. [NIST SP 800-63B](https://pages.nist.gov/800-63-4/sp800-63b.html)
also emphasizes rate limiting as a defence against online guessing. OWASP
recommends slow password-hashing algorithms such as Argon2id, bcrypt, or PBKDF2;
BCrypt is appropriate for this Spring application because it is supported by
Spring Security. [OWASP Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)

### 3.3 Trust boundaries

```text
[Browser / API client] -- untrusted input and bearer credential --> [ResolveX API]
                                                                    |
                                                                    | trusted database connection
                                                                    v
                                                               [PostgreSQL]

[Docker/.env or production secret store] -- secrets --> [ResolveX API]
```

- **Client to API:** Treat every header, path variable, query parameter, request
  body, and JWT as untrusted until validated. TLS/HTTPS is mandatory outside
  local development.
- **API to PostgreSQL:** Only the application database user can cross this
  boundary. End users must never connect directly to PostgreSQL.
- **Secrets to API:** The JWT signing key and database password are trusted only
  when injected through deployment configuration. They must not appear in source
  control, API responses, logs, or Swagger examples.
- **Role to action:** A valid token crosses the identity boundary, but each
  endpoint still has to cross the role and object-access boundaries explicitly.

## 4. Authentication Design

### 4.1 Account lifecycle

The first release should use controlled, administrator-only provisioning:

1. A deployment bootstrap process creates the first ADMIN account securely.
2. An ADMIN creates, enables, disables, and assigns a role to later users.
3. A user signs in through `POST /auth/login` with email and password.
4. ResolveX issues an access token only for an enabled user whose password
   matches.
5. Disabling a user blocks future logins immediately. Per-request account-state
   checks also make a disabled account's previously issued token ineffective.

Public `POST /auth/register` should not be included initially. An incident tool
typically has an identified workforce and should not allow anonymous internet
users to create accounts. Registration can be introduced later if ResolveX
becomes a customer-facing product, with email verification and abuse controls.

### 4.2 Password handling

- Store only `password_hash`, never a plain password or reversible encryption.
- Use Spring Security's `BCryptPasswordEncoder`; each hash includes a salt.
- Do not write passwords, password hashes, authorization headers, or tokens to
  application logs.
- Use a minimum password length policy, reject known-compromised/common
  passwords when provisioning, and permit password manager paste support.
- Do not use arbitrary composition rules such as "one uppercase, one symbol" as
  the only strength control. NIST focuses on password length, blocklists, and
  throttling rather than routine composition rules. [NIST implementation FAQ](https://pages.nist.gov/800-63-4-Implementation-Resources/faqs/index.html)
- Return the same response for unknown email, wrong password, or disabled login
  if disclosure of account existence is a concern: `401 Invalid email or password`.
- Rate-limit login attempts by account and IP address. The exact production
  implementation can live at an API gateway, reverse proxy, or application
  layer; it must be in place before public exposure.

### 4.3 Login API

Proposed public endpoint:

```http
POST /auth/login
Content-Type: application/json

{
  "email": "agent@resolvex.example",
  "password": "user-supplied-secret"
}
```

Successful response shape:

```json
{
  "accessToken": "<JWT>",
  "tokenType": "Bearer",
  "expiresInSeconds": 1800,
  "user": {
    "id": 42,
    "name": "Asha Patel",
    "email": "agent@resolvex.example",
    "role": "AGENT"
  }
}
```

The response does not return `password_hash`, internal security flags, or any
token secret.

### 4.4 JWT design and lifecycle

For the initial single-service deployment, use a strongly random HMAC SHA-256
signing secret supplied by environment configuration. The secret must have at
least 256 bits of randomness. Asymmetric keys (for example, RS256) are useful
later if independent services need to validate tokens without holding the signing
private key; they are unnecessary complexity for the first ResolveX release.

Minimal token claims:

```json
{
  "iss": "resolvex-api",
  "sub": "42",
  "email": "agent@resolvex.example",
  "role": "AGENT",
  "iat": 0,
  "exp": 0,
  "jti": "unique-token-id"
}
```

| Claim | Purpose |
| --- | --- |
| `iss` | Identifies ResolveX as issuer; validation rejects another issuer. |
| `sub` | Stable user ID, not a mutable email address. |
| `email` | Helpful display/identity claim; not authoritative over database state. |
| `role` | Initial authorization hint; current database role remains authoritative. |
| `iat` | Issued-at time for diagnostics and lifecycle checks. |
| `exp` | Hard token expiry. Initial recommendation: 30 minutes. |
| `jti` | Unique ID for future revocation/auditing support. |

Validation must reject a token with a missing or invalid signature, unsupported
algorithm, wrong issuer, expired timestamp, malformed claims, or unknown user.
For each protected request, ResolveX should load the active user and use the
current database role rather than blindly trusting a role embedded in an older
token. This is one small database lookup per protected request, but it makes
role changes and account disabling effective immediately.

Initial scope: **access token only**, 30-minute expiry, no refresh-token
endpoint. This keeps HTTP requests stateless while accepting that logout is
effectively handled by client-side token removal and short expiry. Later, if
long-lived browser sessions are a product need, add rotated refresh tokens stored
as hashed records in a dedicated `refresh_tokens` table. That would introduce
intentional server-side token lifecycle state, but the API itself remains free of
HTTP sessions.

For a browser frontend, do not store JWTs in `localStorage` or `sessionStorage`.
OWASP warns that script running in the origin can read those stores. Prefer an
`HttpOnly`, `Secure`, appropriately `SameSite` cookie or a backend-for-frontend
pattern, and design CSRF protection accordingly. [OWASP Session Management Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html)

## 5. Authorization and RBAC

### 5.1 Initial roles

The proposed roles cover the major operational responsibilities of a small
incident-management team:

| Role | Purpose | What it must not do |
| --- | --- | --- |
| `ADMIN` | Platform administrator and incident supervisor. Manages users and all incidents. | Should not bypass audit logging or database controls. |
| `AGENT` | Operational responder. Creates incidents and works on incidents assigned to them. | Must not manage users, change roles, delete incidents, or update another agent's assignment. |
| `VIEWER` | Stakeholder, requester, or observer. Can read incident data. | Must not create, update, assign, delete, or manage users. |

This model applies the least-privilege and deny-by-default principles recommended
by OWASP. New endpoints must be denied unless an explicit rule grants access.
[OWASP Authorization Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html)

### 5.2 Permission matrix

`Own/assigned` means the AGENT is the current `assigned_to_user_id`. `Any` means
the role can act on every incident.

| Capability | ADMIN | AGENT | VIEWER |
| --- | --- | --- | --- |
| Sign in | Yes | Yes | Yes |
| List and read incidents | Any | Any | Any |
| Create an incident | Yes | Yes | No |
| Update incident content/status/priority | Any | Own/assigned | No |
| Assign or reassign an incident | Any | No | No |
| Delete an incident | Any | No | No |
| Create users | Yes | No | No |
| Enable/disable users | Yes | No | No |
| Change user roles | Yes | No | No |
| View user directory | Yes | Optional, limited directory | No |

The recommended rule is that any authenticated role can view all incidents. If
incident data later becomes tenant-, team-, or confidentiality-scoped, this rule
must change to a membership or group-based visibility check.

### 5.3 Are these roles enough?

Yes, for the initial product scope. They separate platform administration,
incident response, and read-only access without role overlap. Adding roles now
without concrete duties makes policies difficult to review and test.

Potential later roles, only when a real workflow demands them:

| Candidate role | When it becomes justified |
| --- | --- |
| `MANAGER` | A supervisor must assign/approve incidents but must not administer user accounts. |
| `REQUESTER` | External users must create and view only incidents they submitted. |
| `AUDITOR` | Compliance staff need read-only access to incidents and audit events. |
| `TEAM_LEAD` | Several teams need local assignment authority without global ADMIN power. |

Do not create these roles until their exact permissions and responsible business
owner are known.

### 5.4 RBAC database approach: fixed roles now, flexible RBAC later

For three stable roles, a `role` enum column on `users` is the correct first
implementation. It is simple, database-constrained, easy to query, and prevents
ambiguous multi-role combinations. This is still RBAC: the role grants the
application-defined permissions.

Do **not** add `roles`, `permissions`, `user_roles`, and `role_permissions`
tables now. They are appropriate only when administrators must configure
permissions dynamically, a user needs several independent roles, or permission
sets vary by tenant/team. At that point, migrate deliberately to:

```text
roles(id, code, name, description)
permissions(id, code, description)
user_roles(user_id, role_id)
role_permissions(role_id, permission_id)
```

Until that need exists, the four-table model increases migration, query, testing,
and administration complexity without providing a product benefit.

## 6. Users, Ownership, and Assignment Data Model

### 6.1 New `users` table: first-release schema

The table should be created by a new versioned Flyway migration, for example
`V4__add_users_and_incident_assignment.sql` once implementation begins.

```sql
CREATE TABLE users (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_role
        CHECK (role IN ('ADMIN', 'AGENT', 'VIEWER'))
);

CREATE INDEX idx_users_role ON users (role);
CREATE INDEX idx_users_enabled ON users (enabled);
```

| Field | Purpose and rules |
| --- | --- |
| `id` | Internal immutable primary key. Use this for foreign keys and JWT `sub`. |
| `name` | Display name. Required, not an authorization identifier. |
| `email` | Login identifier. Required and unique; normalize consistently before storage and lookup. |
| `password_hash` | BCrypt output only. Never exposed by a response DTO or log. |
| `role` | One fixed RBAC role: `ADMIN`, `AGENT`, or `VIEWER`. |
| `enabled` | Allows an ADMIN to suspend access without deleting historical ownership. |
| `created_at` | Account creation time. |
| `updated_at` | Last account modification time. |

Use a `UserResponse` DTO that omits `password_hash`. Keep a separate
`CreateUserRequest` and `UpdateUserRequest`; neither entity nor request should
be returned directly from a controller.

### 6.2 Changes required to `incidents`

Creating users without associating them to incidents would lose accountability
and make assignment-aware authorization impossible. The migration-safe sequence
adds two initially nullable foreign-key columns, backfills the creator, then
makes the creator mandatory:

```sql
ALTER TABLE incidents
    ADD COLUMN created_by_user_id BIGINT NULL,
    ADD COLUMN assigned_to_user_id BIGINT NULL;

-- After every existing incident has a valid creator:
ALTER TABLE incidents
    ALTER COLUMN created_by_user_id SET NOT NULL,
    ADD CONSTRAINT fk_incidents_created_by_user
        FOREIGN KEY (created_by_user_id) REFERENCES users(id),
    ADD CONSTRAINT fk_incidents_assigned_to_user
        FOREIGN KEY (assigned_to_user_id) REFERENCES users(id);

CREATE INDEX idx_incidents_created_by_user_id
    ON incidents (created_by_user_id);

CREATE INDEX idx_incidents_assigned_to_user_id
    ON incidents (assigned_to_user_id);
```

| Incident field | Required? | Purpose |
| --- | --- | --- |
| `created_by_user_id` | Yes for every new incident | Records the authenticated user who created it; never accept this from a client body. |
| `assigned_to_user_id` | No | Current responsible AGENT. It starts as `NULL` when unassigned. Only ADMIN can assign or reassign in the first-release policy. |

The authenticated principal, not a request field, supplies `created_by_user_id`.
An ADMIN-only assignment endpoint or assignment attribute in an ADMIN-restricted
update request supplies `assigned_to_user_id`.

### 6.3 Migration treatment for existing incidents

The current database already has incidents. A migration cannot add a `NOT NULL`
creator reference until a valid user exists to own older records. The safe
sequence is:

1. Create `users` and insert a controlled bootstrap ADMIN.
2. Add `created_by_user_id` as temporarily nullable and `assigned_to_user_id` as
   nullable.
3. Backfill existing incidents' creator with the bootstrap ADMIN, or preferably
   a clearly labelled historical-import account such as `system@resolvex.local`.
4. Verify no incident has a null creator.
5. Alter `created_by_user_id` to `NOT NULL`.
6. Add foreign keys and indexes.

The final migration should be written only after the bootstrap strategy is
agreed, because production migration code must never invent or expose a real
administrator password. Bootstrap credentials should be supplied securely at
deployment or created through a one-time administrator command.

### 6.4 Assignment history and audit fields

For the first release, current assignment columns are enough for authorization.
For production traceability, plan a later `incident_assignment_history` table:

```text
incident_assignment_history
  id
  incident_id                 -> incidents.id
  previous_assignee_user_id   -> users.id, nullable
  new_assignee_user_id        -> users.id, nullable
  changed_by_user_id          -> users.id
  changed_at
  reason                      -> optional
```

Also consider a generic `audit_events` table later for security and business
events such as login success/failure, user disabled, role changed, incident
created, status changed, priority changed, and assignment changed. Such records
should be protected from ordinary user modification. This is valuable for
incident investigations and compliance, but is not required before the first
secure authentication release.

## 7. Groups and Teams

A group is a collection of users, usually representing a team such as Network,
Database, or Platform Operations. Groups typically require membership and perhaps
group-scoped permissions:

```text
groups(id, name, description, enabled, created_at, updated_at)
user_groups(user_id, group_id, joined_at)
```

An incident could then carry `assigned_group_id`, and an authorization rule might
allow any member of that group to work on it.

### Decision: defer groups

Groups are **not necessary now** because the confirmed first-release workflow
has individual AGENT assignment, a small fixed role set, and no requirement that
several users jointly own a queue. Adding groups prematurely raises unresolved
questions:

- Can a user be in several groups?
- Can an AGENT from any group view every incident or only their group's?
- Does a group lead assign work?
- What happens when an incident is moved between groups?
- Is assignment to a person, group, or both required?

Add groups later when at least one real business rule needs them, such as
team-specific queues, group-level incident visibility, or assignment to an
on-call team. When added, groups should be an authorization boundary, not merely
a display label. At that time, introduce `groups`, `user_groups`, and optionally
`incidents.assigned_group_id`, plus membership-aware authorization tests.

## 8. Spring Security Component Design

The implementation should keep security responsibilities separated:

```text
config/
  SecurityConfig
    - Stateless configuration
    - Public-route allowlist
    - Deny-by-default authorization rules
    - Password encoder bean

security/
  JwtService
    - Creates signed tokens
    - Validates signature, algorithm, issuer, and expiry
  JwtAuthenticationFilter
    - Reads Authorization: Bearer header
    - Establishes authenticated principal after validation
  AuthenticatedUser
    - Current user ID, email, role, and enabled state

entity/
  User
  UserRole

repository/
  UserRepository

service/
  AuthenticationService
    - Login and password verification
  UserService
    - ADMIN-controlled user management
  IncidentAuthorizationService
    - Role and assignment checks for an incident

controller/
  AuthController
  UserController
  IncidentController
```

`SecurityConfig` handles broad HTTP rules. `IncidentAuthorizationService` handles
the business-specific check that an AGENT is assigned to a particular incident.
Keeping this logic out of the controller makes it reusable and straightforward to
test.

## 9. Route Policy

| Route or route family | Authentication | Authorization |
| --- | --- | --- |
| `POST /auth/login` | Public | Public, rate limited |
| `GET /health` | Public | Public health indicator only; no secrets |
| `/swagger-ui/**`, `/v3/api-docs/**` | Public in development | Consider restricting in production |
| `GET /incidents`, `GET /incidents/{id}` | Required | ADMIN, AGENT, VIEWER |
| `POST /incidents` | Required | ADMIN, AGENT; creator comes from token |
| `PUT /incidents/{id}` | Required | ADMIN, or assigned AGENT only |
| Assignment/reassignment endpoint | Required | ADMIN only initially |
| `DELETE /incidents/{id}` | Required | ADMIN only |
| `/users/**` | Required | ADMIN only, except a future safe `GET /users/me` |

The current controller supports both `/incident` and `/incidents`; security rules
must cover both aliases until the legacy route is removed.

Use `401 Unauthorized` when authentication is missing, malformed, invalid, or
expired. Use `403 Forbidden` when authentication is valid but the current user
lacks the role or assignment required by the action. Error responses must not
include token contents, secrets, password information, or implementation stack
traces.

## 10. Configuration, Docker, and Operational Security

Add environment variables to `.env.example` only as safe placeholders:

```dotenv
JWT_SECRET=replace-with-a-cryptographically-random-32-byte-or-longer-secret
JWT_ISSUER=resolvex-api
JWT_EXPIRATION_MINUTES=30
```

Docker Compose will pass them to the `app` service, and Spring configuration will
bind them to configuration such as `app.jwt.secret`, `app.jwt.issuer`, and
`app.jwt.expiration-minutes`.

Requirements:

- `.env` remains ignored by Git. Commit `.env.example`, never a real secret.
- Use different secrets per environment. Do not reuse local credentials in test
  or production.
- Rotate the JWT secret through a controlled deployment procedure. Rotating a
  single secret invalidates existing access tokens; planned key rotation can
  support a temporary previous key if uptime demands it.
- Use HTTPS behind a reverse proxy or load balancer in every non-local
  deployment. Bearer tokens and passwords must never cross public networks over
  plain HTTP.
- Do not expose PostgreSQL's port publicly in a production network unless there
  is a tightly controlled operational requirement.
- Run the application database user with only the rights ResolveX needs; it
  should not be a PostgreSQL superuser.

## 11. Implementation Plan

### Phase 1: foundation

1. Add Spring Security and a maintained JWT library.
2. Add JWT configuration and safe `.env.example` placeholders.
3. Add user entity, `UserRole`, repository, DTOs, and a Flyway migration.
4. Create the first administrator through a controlled bootstrap procedure.
5. Add creator and assignee foreign keys to incidents using the safe backfill
   sequence described above.

### Phase 2: sign-in and route protection

1. Implement BCrypt password handling and `POST /auth/login`.
2. Implement token creation and strict validation.
3. Configure stateless Spring Security and public route allowlist.
4. Require authentication for all incident routes.
5. Add role-level route authorization.

### Phase 3: ownership and administration

1. Populate `created_by_user_id` from the authenticated user on incident create.
2. Add ADMIN-only assignment/reassignment behaviour.
3. Enforce assigned-AGENT-only incident updates.
4. Add ADMIN-only user creation, role changes, and enable/disable operations.
5. Document bearer authentication in OpenAPI/Swagger.

### Phase 4: hardening and operations

1. Add login rate limiting and monitoring.
2. Add security-event and assignment-history audit logging if required.
3. Add refresh-token rotation only if long-lived sessions are a confirmed need.
4. Reassess groups/teams when team-level ownership is introduced.
5. Consider MFA for ADMIN users and high-risk deployments.

## 12. Verification Strategy

Extend the existing Testcontainers PostgreSQL integration suite with these tests:

- Correct password produces a valid access token.
- Unknown email, incorrect password, and disabled account fail safely.
- Requests with no token, malformed token, expired token, wrong issuer, and bad
  signature receive `401`.
- A VIEWER can read incidents but cannot create, update, assign, delete, or
  manage users.
- An AGENT can create an incident and update only an incident assigned to them.
- An AGENT cannot update another AGENT's incident by changing the path ID.
- An ADMIN can create users, manage roles, assign any incident, and delete an
  incident.
- A user disabled after token issuance cannot continue using that token.
- Incident creation records the authenticated creator, ignoring any forged
  creator ID in a request body.
- User and incident response DTOs never contain password hashes or token secrets.

Testing object-level authorization is mandatory: it is the safety net against
the common API error of checking a role but not whether the caller is entitled to
the particular record.

## 13. Deferred Decisions

These features are intentionally deferred, not forgotten:

| Deferred feature | Revisit when |
| --- | --- |
| User groups and team queues | Incidents are routed to multiple operational teams. |
| Dynamic roles and permissions tables | Permissions must be configured without a code deployment or users need multiple roles. |
| Refresh tokens and logout revocation | Product requires sessions longer than the short access-token lifetime. |
| MFA | ADMIN accounts, compliance obligations, or risk profile require stronger assurance. |
| Self-registration and email verification | ResolveX becomes external/customer-facing. |
| Assignment history and audit events | Compliance, investigations, or reporting require a full change trail. |
| Multi-tenancy | One ResolveX deployment serves independent organizations. |

The next implementation decision is to confirm this first-release policy:
**ADMIN-only user provisioning, three fixed roles, ADMIN-only assignment,
AGENT-only work on assigned incidents, and no groups yet.**
