-- Preserves accountability for incidents created before ResolveX had user accounts.
-- This disabled account is reserved for historical data and must never be used to log in.
INSERT INTO users (name, email, password_hash, role, enabled, created_at, updated_at)
VALUES (
    'Historical Import',
    'system@resolvex.local',
    '$2b$12$c.Z9Sv/9zVlJ4elyZ6vAbuohYBEc3XP7/Y2r6EgvY.sn83KLKjWSS',
    'VIEWER',
    FALSE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (email) DO NOTHING;

ALTER TABLE incidents
    ADD COLUMN created_by_user_id BIGINT,
    ADD COLUMN assigned_to_user_id BIGINT;

UPDATE incidents
SET created_by_user_id = (
    SELECT id
    FROM users
    WHERE email = 'system@resolvex.local'
)
WHERE created_by_user_id IS NULL;

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
