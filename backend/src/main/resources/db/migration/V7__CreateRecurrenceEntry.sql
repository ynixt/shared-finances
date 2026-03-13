CREATE TABLE recurrence_entry (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    wallet_event_id UUID NOT NULL,
    wallet_item_id UUID NOT NULL,
    value NUMERIC(12,2) NOT NULL,
    next_bill_date DATE,
    last_bill_date DATE,
    CONSTRAINT fk_recurrence_entry_event FOREIGN KEY (wallet_event_id) REFERENCES recurrence_event(id) ON DELETE CASCADE,
    CONSTRAINT fk_recurrence_entry_wallet_item FOREIGN KEY (wallet_item_id) REFERENCES wallet_item(id) ON DELETE CASCADE
);

CREATE INDEX idx_recurrence_entry_primary ON recurrence_entry(wallet_event_id, next_bill_date, last_bill_date);
CREATE INDEX idx_recurrence_entry_event_wallet_item_id ON recurrence_entry(wallet_event_id, wallet_item_id);
