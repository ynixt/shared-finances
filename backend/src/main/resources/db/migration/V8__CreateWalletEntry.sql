CREATE TABLE wallet_entry (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    type TEXT NOT NULL,
    name CITEXT NOT NULL CHECK (char_length(name) <= 255),
    description TEXT,
    value NUMERIC(12,2) NOT NULL,
    category_id UUID,
    user_id UUID,
    group_id UUID,
    origin_id UUID NOT NULL,
    target_id UUID,
    tags TEXT[],
    observations TEXT,
    date DATE NOT NULL,
    confirmed BOOLEAN NOT NULL,
    origin_bill_id UUID,
    target_bill_id UUID,
    installment INT,
    recurrence_config_id UUID,
    periodicity TEXT,
    CONSTRAINT fk_w_origin FOREIGN KEY (origin_id) REFERENCES wallet_item(id) ON DELETE SET NULL,
    CONSTRAINT fk_w_target FOREIGN KEY (target_id) REFERENCES wallet_item(id) ON DELETE SET NULL,
    CONSTRAINT fk_w_entry_category FOREIGN KEY (category_id) REFERENCES wallet_entry_category(id) ON DELETE SET NULL,
    CONSTRAINT fk_w_entry_user FOREIGN KEY (user_id) REFERENCES "users"(id) ON DELETE SET NULL,
    CONSTRAINT fk_w_entry_group FOREIGN KEY ("group_id") REFERENCES "group"(id) ON DELETE SET NULL,
    CONSTRAINT fk_w_entry_origin_bill_id FOREIGN KEY (origin_bill_id) REFERENCES credit_card_bill(id) ON DELETE SET NULL,
    CONSTRAINT fk_w_entry_target_bill_id FOREIGN KEY (target_bill_id) REFERENCES credit_card_bill(id) ON DELETE SET NULL,
    CONSTRAINT fk_w_entry_recurrence FOREIGN KEY (recurrence_config_id) REFERENCES entry_recurrence_config(id)
);

CREATE INDEX idx_wallet_entry_user_date_id ON wallet_entry(user_id, date DESC, id DESC) WHERE user_id IS NOT NULL;
CREATE INDEX idx_wallet_entry_group_date_id ON wallet_entry(group_id, date DESC, id DESC) WHERE group_id IS NOT NULL;
CREATE INDEX idx_wallet_entry_user_origin_id ON wallet_entry(user_id, origin_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_wallet_entry_group_origin_id ON wallet_entry(group_id, origin_id) WHERE group_id IS NOT NULL;
CREATE INDEX idx_wallet_entry_user_target_id ON wallet_entry(user_id, target_id) WHERE user_id IS NOT NULL AND target_id IS NOT NULL;
CREATE INDEX idx_wallet_entry_group_target_id ON wallet_entry(group_id, target_id) WHERE group_id IS NOT NULL AND target_id IS NOT NULL;
CREATE INDEX idx_wallet_entry_origin_bill ON wallet_entry(origin_bill_id);
CREATE INDEX idx_wallet_entry_target_bill ON wallet_entry(target_bill_id);
CREATE INDEX idx_wallet_entry_tags ON wallet_entry USING GIN(tags);

CREATE INDEX idx_wallet_entry_value_positive_user_id
    ON wallet_entry(user_id, value)
    WHERE value > 0 AND user_id IS NOT NULL;
CREATE INDEX idx_wallet_entry_value_positive_group_id
    ON wallet_entry(group_id, value)
    WHERE value > 0 AND group_id IS NOT NULL;
CREATE INDEX idx_wallet_entry_value_negative_user_id
    ON wallet_entry(user_id, value)
    WHERE value < 0 AND user_id IS NOT NULL;
CREATE INDEX idx_wallet_entry_value_negative_group_id
    ON wallet_entry(group_id, value)
    WHERE value < 0 AND group_id IS NOT NULL;
