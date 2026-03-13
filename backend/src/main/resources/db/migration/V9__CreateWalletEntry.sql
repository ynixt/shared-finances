CREATE TABLE wallet_entry (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    value NUMERIC(12,2) NOT NULL,
    wallet_event_id UUID NOT NULL,
    wallet_item_id UUID NOT NULL,
    bill_id UUID,
    CONSTRAINT fk_w_entry_w_event FOREIGN KEY (wallet_event_id) REFERENCES wallet_event(id) ON DELETE CASCADE,
    CONSTRAINT fk_w_entry_wallet_item FOREIGN KEY (wallet_item_id) REFERENCES wallet_item(id),
    CONSTRAINT fk_w_entry_bill FOREIGN KEY (bill_id) REFERENCES credit_card_bill(id)
);

CREATE INDEX idx_wallet_entry_main ON wallet_entry(wallet_event_id, wallet_item_id, bill_id);
CREATE INDEX idx_wallet_entry_item_bill ON wallet_entry(wallet_item_id, bill_id);
CREATE INDEX idx_wallet_entry_event_bill ON wallet_entry(wallet_event_id, bill_id);


CREATE INDEX idx_wallet_entry_value_positive_wallet_event_id
    ON wallet_entry(wallet_event_id, value)
    WHERE value > 0;

CREATE INDEX idx_wallet_entry_value_negative_wallet_event_id
    ON wallet_entry(wallet_event_id, value)
    WHERE value < 0;
