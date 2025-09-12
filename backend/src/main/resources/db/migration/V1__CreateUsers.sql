CREATE TABLE "users"
(
    id         UUID PRIMARY KEY,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    external_id TEXT NOT NULL UNIQUE,
    email      CITEXT NOT NULL CHECK (char_length(email) <= 320) UNIQUE,
    first_name       VARCHAR(255) NOT NULL,
    last_name       VARCHAR(255) NOT NULL,
    lang       VARCHAR(8)  NOT NULL,
    default_currency VARCHAR(3)
);
