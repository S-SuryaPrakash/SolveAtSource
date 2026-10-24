# ResolveX

ResolveX is an incident-management REST API built with Spring Boot, PostgreSQL,
Flyway, Docker, and OpenAPI. It currently supports incident creation, retrieval,
updates, deletion, filtering, sorting, pagination, and grouping.

This document records the proposed architecture for the next major feature:
authentication and authorization. It is a design decision document; the security
feature described below is not implemented yet.

## Goals

- Authenticate a user safely without exposing passwords.
- Restrict incident operations according to a user's role.
- Keep the API stateless and suitable for a browser or another API client.
- Store users and roles in PostgreSQL so they can be managed in production.
- Make the design testable, documentable in Swagger, and compatible with Docker.

## Proposed Architecture

ResolveX will use JSON Web Tokens (JWTs) and role-based access control (RBAC).

```text
Client                         ResolveX API                      PostgreSQL
  |                                  |                                |
  | POST /auth/login                 |                                |
  | email + password                 |---- load user by email ------->|
  |                                  |<--- user + password hash -------|
  |                                  | verify BCrypt password          |
  |<---- signed JWT -----------------|                                |
  |
  | GET /incidents                   |
  | Authorization: Bearer <JWT>      |
  |--------------------------------->| validate JWT and load role     |
  |                                  | authorize endpoint access      |
  |<---- incident response ----------|                                |
```

JWTs are suitable here because each request carries the caller's identity. The
API does not need server-side HTTP session storage, which works well when the
application is deployed as one or more Docker containers.

## Authentication and Authorization

Authentication establishes who the caller is. A login request supplies an email
and password; the API verifies the password against its stored BCrypt hash and
returns a JWT.

Authorization establishes what that authenticated caller may do. Spring Security
will read the role from the validated JWT and enforce access rules before the
request reaches the controller or service layer.

## Roles and Endpoint Access

The initial role model is deliberately small and can be expanded later.

| Role | Intended responsibility |
| --- | --- |
| `ADMIN` | Full incident management and user administration. |
| `AGENT` | Work on incidents: create, view, and update them. |
| `VIEWER` | Read incident information only. |

| Endpoint | ADMIN | AGENT | VIEWER | Unauthenticated |
| --- | :---: | :---: | :---: | :---: |
| `POST /auth/login` | ✓ | ✓ | ✓ | ✓ |
| `POST /auth/register` (if enabled) | — | — | — | Design decision |
| `GET /incidents` and `GET /incidents/{id}` | ✓ | ✓ | ✓ | ✗ |
| `POST /incidents` | ✓ | ✓ | ✗ | ✗ |
| `PUT /incidents/{id}` | ✓ | ✓ | ✗ | ✗ |
| `DELETE /incidents/{id}` | ✓ | ✗ | ✗ | ✗ |
| User-management endpoints | ✓ | ✗ | ✗ | ✗ |
| Swagger/OpenAPI and health endpoints | Public | Public | Public | ✓ |

`/incident` is a legacy alias currently supported by the controller. The same
authorization rules will apply to both `/incident` and `/incidents`.

## User Data Model

User records will be persisted through a Flyway migration and mapped to a JPA
entity. Passwords must never be stored or returned as plain text.

```text
users
├── id              BIGINT primary key
├── name            VARCHAR
├── email           VARCHAR unique, not null
├── password_hash   VARCHAR, not null
├── role            VARCHAR, not null (ADMIN, AGENT, VIEWER)
├── enabled         BOOLEAN, not null
├── created_at      TIMESTAMP, not null
└── updated_at      TIMESTAMP, not null
```

The database stores enum names such as `ADMIN`; API DTOs may use the same values
initially. A unique database constraint on `email` prevents duplicate accounts.

## Token Design

The signed access token should contain only the information required to identify
and authorize the caller:

```json
{
  "sub": "user@example.com",
  "userId": 42,
  "role": "AGENT",
  "iat": 0,
  "exp": 0
}
```

- `sub` identifies the user, normally by email.
- `userId` provides a stable internal identifier.
- `role` supplies the authority used by Spring Security.
- `iat` and `exp` record issuance and expiry times.

The token must not contain the password, password hash, database credentials, or
any other secret. Production access tokens should be short lived (for example,
15 to 60 minutes). Refresh-token support can be added as a separate later phase
once the core sign-in flow is established.

## Security Controls

- Hash passwords using Spring Security's `BCryptPasswordEncoder`.
- Sign JWTs with a strong secret supplied through environment variables, never
  hard-coded or committed to Git.
- Configure the application as stateless; disable server-side sessions.
- Return `401 Unauthorized` for an absent, expired, malformed, or invalid token.
- Return `403 Forbidden` when a valid user lacks the required role.
- Allow only the login, API documentation, and health endpoints to be public.
- Keep login error messages generic so they do not reveal whether an email exists.
- Validate request DTOs and add rate limiting at an API gateway or reverse proxy
  when the application is exposed publicly.
- Use HTTPS in every non-local environment because bearer tokens must not travel
  over an unencrypted connection.

## Configuration and Docker

The JWT signing key and token lifetime will be external configuration. Local
values belong in the ignored `.env` file, while `.env.example` documents the
required variable names without including a real secret.

```dotenv
JWT_SECRET=replace-with-a-long-random-secret
JWT_EXPIRATION_MINUTES=30
```

Docker Compose will pass these values to the application container. The Spring
configuration will map them to application properties such as `app.jwt.secret`
and `app.jwt.expiration-minutes`.

## Planned Components

```text
config/
  SecurityConfig              HTTP security rules and public routes
  JwtAuthenticationFilter     Extracts and validates Bearer tokens
  JwtService                  Creates and validates JWTs

entity/
  User                        Persisted account
  UserRole                    ADMIN, AGENT, VIEWER

repository/
  UserRepository              Finds users by email

service/
  AuthenticationService       Login and password verification
  CustomUserDetailsService    Supplies Spring Security user details

controller/
  AuthController              /auth/login and selected account endpoints

dto/
  LoginRequest, AuthResponse, UserResponse
```

Exact class names can change during implementation, but separating token logic,
security configuration, persistence, and HTTP DTOs keeps responsibilities clear.

## Implementation Phases

1. Add Spring Security and the selected JWT library dependencies.
2. Add `users` and role schema through a versioned Flyway migration.
3. Implement the user entity, role enum, repository, and safe DTOs.
4. Establish the first administrator account through a controlled seed or
   administrator-only provisioning process.
5. Implement login with BCrypt verification and signed JWT issuance.
6. Add JWT validation filter and stateless Spring Security configuration.
7. Apply role rules to incident endpoints.
8. Add user provisioning as either public registration or ADMIN-only creation,
   depending on the chosen product policy.
9. Add integration tests for successful authentication, `401` responses, and
   `403` role restrictions.
10. Add Swagger bearer-token support and update Docker environment configuration.

## Decisions Still Needed

Before implementation, ResolveX needs to choose:

1. **Account creation:** Can anyone self-register, or may only an administrator
   provision accounts? For an internal incident tool, ADMIN-only provisioning is
   the safer default.
2. **Role model:** Confirm `ADMIN`, `AGENT`, and `VIEWER`, or define additional
   roles such as an incident manager or requester.
3. **Ownership rules:** Should an AGENT be able to update every incident, or only
   incidents assigned to that AGENT? Assignment is not currently part of the
   incident data model and would be a future enhancement.
4. **Token lifecycle:** Start with access tokens only, or add refresh tokens and
   logout/revocation in the first security release? Access tokens only is the
   recommended initial scope.

## Testing Strategy

Existing Testcontainers-based PostgreSQL integration tests will be extended to
cover the security boundary:

- Login succeeds with correct credentials and produces a token.
- Login fails without revealing whether the email or password was wrong.
- A request without a token returns `401`.
- A request with an invalid or expired token returns `401`.
- A VIEWER can read but cannot create, update, or delete an incident.
- An AGENT can create and update but cannot delete an incident.
- An ADMIN can perform every permitted operation.

This provides confidence that access is enforced by the running application,
rather than relying only on controller annotations or manual Swagger testing.
