CREATE TABLE wallet_item (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    user_id  UUID NOT NULL,
    type TEXT NOT NULL,
    name CITEXT NOT NULL CHECK (char_length(name) <= 255),
    currency VARCHAR(3) NOT NULL,
    enabled BOOLEAN NOT NULL,
    balance NUMERIC(12,2) NOT NULL,
    total_limit NUMERIC(12,2),
    due_day INT,
    days_between_due_and_closing INT,
    due_on_next_business_day BOOLEAN,

    CONSTRAINT fk_credit_card_users
        FOREIGN KEY (user_id) REFERENCES "users" (id)
            ON DELETE CASCADE,

    CONSTRAINT chk_payment_type_fields CHECK (
        (
            type = 'CREDIT_CARD'
            AND total_limit IS NOT NULL
            AND due_day IS NOT NULL
            AND days_between_due_and_closing IS NOT NULL
            AND due_on_next_business_day IS NOT NULL
        )
        OR (
            type = 'BANK_ACCOUNT'
            AND total_limit IS NULL
            AND due_day IS NULL
            AND days_between_due_and_closing IS NULL
            AND due_on_next_business_day IS NULL
        )
    )
);

CREATE INDEX idx_wallet_item_user_id ON wallet_item (user_id);
CREATE INDEX idx_wallet_item_type_user_id ON wallet_item (type, user_id);