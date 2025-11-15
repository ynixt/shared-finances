ALTER TABLE wallet_entry_category ADD column parent_id UUID;
CREATE INDEX idx_wallet_entry_category_parent_id ON wallet_entry_category(parent_id);