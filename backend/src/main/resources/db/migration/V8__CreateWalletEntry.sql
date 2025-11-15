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
    bill_id UUID,
    installment_config_id UUID,
    installment INT,
    recurrence_config_id UUID,
    wallet_item_destination_id UUID,
    CONSTRAINT fk_w_origin FOREIGN KEY (origin_id) REFERENCES wallet_item(id) ON DELETE SET NULL,
    CONSTRAINT fk_w_target FOREIGN KEY (target_id) REFERENCES wallet_item(id) ON DELETE SET NULL,
    CONSTRAINT fk_w_entry_category FOREIGN KEY (category_id) REFERENCES wallet_entry_category(id) ON DELETE SET NULL,
    CONSTRAINT fk_w_entry_user FOREIGN KEY (user_id) REFERENCES "users"(id) ON DELETE SET NULL,
    CONSTRAINT fk_w_entry_group FOREIGN KEY ("group_id") REFERENCES "group"(id) ON DELETE SET NULL,
    CONSTRAINT fk_w_entry_bill FOREIGN KEY (bill_id) REFERENCES credit_card_bill(id) ON DELETE SET NULL,
    CONSTRAINT fk_w_entry_installment FOREIGN KEY (installment_config_id) REFERENCES entry_installment_config(id),
    CONSTRAINT fk_w_entry_recurrence FOREIGN KEY (recurrence_config_id) REFERENCES entry_recurrence_config(id),

    CONSTRAINT chk_payment_type_fields CHECK (
        (
            type = 'CREDIT_CARD'
                AND bill_id IS NOT NULL
                AND target_id IS NULL
            )
            OR (
            type = 'TRANSFER'
                AND target_id IS NOT NULL
            )
        )
);

CREATE INDEX idx_wallet_entry_user ON wallet_entry(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_wallet_entry_group ON wallet_entry(group_id) WHERE group_id IS NOT NULL;
CREATE INDEX idx_wallet_entry_user_date ON wallet_entry(user_id, date DESC) WHERE user_id IS NOT NULL;
CREATE INDEX idx_wallet_entry_group_date ON wallet_entry(group_id, date DESC) WHERE group_id IS NOT NULL;
CREATE INDEX idx_wallet_entry_user_origin_id ON wallet_entry(user_id, origin_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_wallet_entry_group_origin_id ON wallet_entry(group_id, origin_id) WHERE group_id IS NOT NULL;
CREATE INDEX idx_wallet_entry_user_target_id ON wallet_entry(user_id, target_id) WHERE user_id IS NOT NULL AND target_id IS NOT NULL;
CREATE INDEX idx_wallet_entry_group_target_id ON wallet_entry(group_id, target_id) WHERE group_id IS NOT NULL AND target_id IS NOT NULL;
CREATE INDEX idx_wallet_entry_bill ON wallet_entry(bill_id);
CREATE INDEX idx_wallet_entry_value_positive_user_id
    ON wallet_entry(value, user_id)
    WHERE value > 0 AND user_id IS NOT NULL;
CREATE INDEX idx_wallet_entry_value_negative_user_id
    ON wallet_entry(value)
    WHERE value < 0 AND user_id IS NOT NULL;
CREATE INDEX idx_wallet_entry_value_positive_group_id
    ON wallet_entry(value, group_id)
    WHERE value > 0 AND group_id IS NOT NULL;
CREATE INDEX idx_wallet_entry_value_negative_group_id
    ON wallet_entry(value)
    WHERE value < 0 AND group_id IS NOT NULL;
