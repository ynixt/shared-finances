CREATE TABLE "group"
(
    id         UUID PRIMARY KEY,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    name       VARCHAR(255) NOT NULL
);
