CREATE TABLE IF NOT EXISTS session
(
    id         UUID PRIMARY KEY,
    user_id    uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz,
    user_agent text,
    ip         inet
);

CREATE INDEX IF NOT EXISTS ix_session_user_id_created_at
    ON session (user_id, created_at DESC);