CREATE INDEX IF NOT EXISTS idx_wallet_item_visible_user_id
    ON wallet_item (user_id)
    WHERE enabled = true AND show_on_dashboard = true;
