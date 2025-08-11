CREATE TABLE bank_account (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    user_id  UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL,
    balance NUMERIC(12,2) NOT NULL,

    CONSTRAINT fk_bank_account_users
        FOREIGN KEY (user_id) REFERENCES "users" (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_bank_account_user_id ON bank_account (user_id);