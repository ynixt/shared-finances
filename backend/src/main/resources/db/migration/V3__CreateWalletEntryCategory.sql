CREATE TABLE wallet_entry_category (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    name CITEXT NOT NULL CHECK (char_length(name) <= 255),
    color VARCHAR(9) NOT NULL,
    icon VARCHAR(64) NOT NULL,
    user_id UUID,
    group_id UUID,
    CONSTRAINT fk_wallet_entry_category_user FOREIGN KEY (user_id) REFERENCES "users"(id),
    CONSTRAINT fk_wallet_entry_category_group FOREIGN KEY ("group_id") REFERENCES "group"(id)
);

CREATE INDEX idx_wallet_entry_category_user ON wallet_entry_category(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_wallet_entry_category_group ON wallet_entry_category(group_id)  WHERE group_id IS NOT NULL;