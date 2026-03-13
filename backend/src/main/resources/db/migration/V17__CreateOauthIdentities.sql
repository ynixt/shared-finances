CREATE TABLE IF NOT EXISTS oauth_identities
(
    id                UUID PRIMARY KEY,
    user_id           uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,

    provider          text        NOT NULL,
    provider_subject  text        NOT NULL,

    email_at_provider text        NULL,

    created_at        timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT uq_oauth_provider_subject UNIQUE (provider, provider_subject)
);

CREATE INDEX IF NOT EXISTS ix_oauth_identities_user_id
    ON oauth_identities (user_id);

CREATE INDEX IF NOT EXISTS ix_oauth_identities_provider_email
    ON oauth_identities (provider, email_at_provider);