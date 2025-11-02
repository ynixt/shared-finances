CREATE TABLE transfer_entry (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    name CITEXT NOT NULL CHECK (char_length(name) <= 255),
    description TEXT,
    value NUMERIC(12,2) NOT NULL,
    category_id UUID,
    user_id UUID,
    group_id UUID,
    tags TEXT[],
    observations TEXT,
    date DATE NOT NULL,
    confirmed BOOLEAN NOT NULL,
    user_destination_id UUID,
    CONSTRAINT fk_tc_entry_category FOREIGN KEY (category_id) REFERENCES wallet_entry_category(id) ON DELETE SET NULL,
    CONSTRAINT fk_tc_entry_user FOREIGN KEY (user_id) REFERENCES "users"(id) ON DELETE SET NULL,
    CONSTRAINT fk_tc_entry_group FOREIGN KEY ("group_id") REFERENCES "group"(id) ON DELETE SET NULL,
    CONSTRAINT fk_tc_entry_user_destination_id FOREIGN KEY (user_destination_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_transfer_entry_user ON transfer_entry(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_transfer_entry_group ON transfer_entry(group_id) WHERE group_id IS NOT NULL;
CREATE INDEX idx_transfer_entry_user_date ON transfer_entry(user_id, date DESC) WHERE user_id IS NOT NULL;
CREATE INDEX idx_transfer_entry_group_date ON transfer_entry(group_id, date DESC) WHERE group_id IS NOT NULL;
CREATE INDEX idx_transfer_entry_bill ON transfer_entry(user_destination_id);
