CREATE TABLE IF NOT EXISTS refresh_tokens
(
    id         UUID PRIMARY KEY,
    session_id     uuid        NOT NULL REFERENCES session (id) ON DELETE CASCADE,
    token_hash  bytea       NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now(),
    expires_at  timestamptz NOT NULL,

    CONSTRAINT refresh_tokens_expires_after_created
        CHECK (expires_at > created_at)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_refresh_tokens_token_hash
    ON refresh_tokens (token_hash);

CREATE INDEX IF NOT EXISTS ix_refresh_tokens_session_id_created_at
    ON refresh_tokens (session_id, created_at DESC);

CREATE INDEX IF NOT EXISTS ix_refresh_tokens_expires_at
    ON refresh_tokens (expires_at);