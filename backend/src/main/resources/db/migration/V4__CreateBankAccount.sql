CREATE TABLE bank_account (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    user_id  UUID NOT NULL,
    name CITEXT NOT NULL CHECK (char_length(name) <= 255),
    currency VARCHAR(3) NOT NULL,
    enabled BOOLEAN NOT NULL,
    balance NUMERIC(12,2) NOT NULL,

    CONSTRAINT fk_bank_account_users
        FOREIGN KEY (user_id) REFERENCES "users" (id)
            ON DELETE CASCADE
);

CREATE UNIQUE INDEX idx_bank_account_user_id_name ON bank_account (user_id, name);