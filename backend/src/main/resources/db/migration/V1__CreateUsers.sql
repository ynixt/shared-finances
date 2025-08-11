CREATE TABLE "users"
(
    id         UUID PRIMARY KEY,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    external_id TEXT NOT NULL UNIQUE,
    email      VARCHAR(320) NOT NULL UNIQUE,
    first_name       VARCHAR(255) NOT NULL,
    last_name       VARCHAR(255) NOT NULL,
    photo_url  VARCHAR(255),
    lang       VARCHAR(8)  NOT NULL
);
