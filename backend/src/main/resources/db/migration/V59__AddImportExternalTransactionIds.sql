ALTER TABLE wallet_event
    ADD COLUMN external_transaction_id VARCHAR(255);

ALTER TABLE recurrence_event
    ADD COLUMN external_transaction_id VARCHAR(255);

CREATE INDEX idx_wallet_event_external_transaction_id
    ON wallet_event(external_transaction_id)
    WHERE external_transaction_id IS NOT NULL;

CREATE INDEX idx_recurrence_event_external_transaction_id
    ON recurrence_event(external_transaction_id)
    WHERE external_transaction_id IS NOT NULL;
