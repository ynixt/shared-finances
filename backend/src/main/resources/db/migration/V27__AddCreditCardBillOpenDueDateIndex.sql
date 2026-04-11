CREATE INDEX idx_credit_card_bill_open_due_date
    ON credit_card_bill (due_date, credit_card_id)
    WHERE value < 0;
