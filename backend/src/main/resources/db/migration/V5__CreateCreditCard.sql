CREATE TABLE credit_card (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    user_id  UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    enabled BOOLEAN NOT NULL,
    total_limit NUMERIC(12,2) NOT NULL,
    available_limit NUMERIC(12,2) NOT NULL,
    due_day INT NOT NULL,
    days_between_due_and_closing INT NOT NULL,
    due_on_next_business_day BOOLEAN NOT NULL,

    CONSTRAINT fk_credit_card_users
        FOREIGN KEY (user_id) REFERENCES "users" (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_credit_card_user_id ON credit_card (user_id);