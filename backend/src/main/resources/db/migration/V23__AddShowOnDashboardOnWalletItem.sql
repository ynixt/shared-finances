ALTER TABLE wallet_item
    ADD COLUMN show_on_dashboard BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_wallet_item_show_on_dashboard ON wallet_item (show_on_dashboard);
