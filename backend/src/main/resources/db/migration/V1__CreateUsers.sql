CREATE TABLE "users"
(
    id         UUID PRIMARY KEY,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    email      CITEXT NOT NULL CHECK (char_length(email) <= 320) UNIQUE,
    password_hash TEXT,
    first_name       VARCHAR(255) NOT NULL,
    last_name       VARCHAR(255) NOT NULL,
    lang       VARCHAR(8)  NOT NULL,
    tmz TEXT NOT NULL,
    default_currency VARCHAR(3),
    email_verified BOOLEAN DEFAULT FALSE NOT NULL,
    mfa_enabled BOOLEAN DEFAULT FALSE NOT NULL,
    totp_secret TEXT,
    photo_url TEXT
);
