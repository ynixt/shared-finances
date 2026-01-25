CREATE TABLE IF NOT EXISTS mfa_enrollments
(
    id         uuid PRIMARY KEY,
    user_id    uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    secret_enc text        NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    expires_at timestamptz NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_mfa_enrollments_user_id ON mfa_enrollments (user_id);
CREATE INDEX IF NOT EXISTS idx_mfa_enrollments_expires_at ON mfa_enrollments (expires_at);


CREATE TABLE IF NOT EXISTS mfa_challenges
(
    id         uuid PRIMARY KEY,
    user_id    uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    user_agent text        NULL,
    ip         inet        NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    expires_at timestamptz NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_mfa_challenges_user_id ON mfa_challenges (user_id);
CREATE INDEX IF NOT EXISTS idx_mfa_challenges_expires_at ON mfa_challenges (expires_at);

CREATE TABLE IF NOT EXISTS mfa_recovery_codes
(
    id         uuid PRIMARY KEY,
    user_id    uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at timestamptz NOT NULL DEFAULT now(),
    code_hash  text        NOT NULL,
    used_at    timestamptz NULL
);

CREATE INDEX IF NOT EXISTS idx_mfa_recovery_codes_user_id ON mfa_recovery_codes (user_id);
CREATE INDEX IF NOT EXISTS idx_mfa_recovery_codes_user_id_not_used ON mfa_recovery_codes (user_id) where used_at is null;