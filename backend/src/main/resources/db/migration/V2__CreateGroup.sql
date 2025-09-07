CREATE TABLE "group"
(
    id         UUID PRIMARY KEY,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    name       CITEXT NOT NULL CHECK (char_length(name) <= 255)
);

CREATE INDEX idx_group_name ON "group" (name);