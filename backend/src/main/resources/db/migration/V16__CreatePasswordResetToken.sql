CREATE TABLE IF NOT EXISTS password_reset_tokens
(
    id         UUID PRIMARY KEY,
    user_id    uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash bytea       NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    expires_at timestamptz NOT NULL,
    used_at    timestamptz NULL,

    CONSTRAINT password_reset_tokens_expires_after_created
        CHECK (expires_at > created_at)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_password_reset_tokens_token_hash
    ON password_reset_tokens (token_hash);

CREATE INDEX IF NOT EXISTS ix_password_reset_tokens_user_id_created_at
    ON password_reset_tokens (user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS ix_password_reset_tokens_user_id_unused
    ON password_reset_tokens (user_id)
    WHERE used_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_password_reset_tokens_expires_at
    ON password_reset_tokens (expires_at);