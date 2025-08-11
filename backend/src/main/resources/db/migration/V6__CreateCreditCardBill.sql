CREATE TABLE credit_card_bill (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    credit_card_id UUID NOT NULL,
    due_date DATE NOT NULL,
    closing_date DATE NOT NULL,
    payed BOOLEAN NOT NULL,
    value NUMERIC(10,2) NOT NULL,
    CONSTRAINT fk_credit_card_bill_card
        FOREIGN KEY (credit_card_id) REFERENCES credit_card(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_credit_card_bill_card ON credit_card_bill(credit_card_id);
CREATE INDEX idx_credit_card_bill_due_date ON credit_card_bill(due_date);
