CREATE TABLE credit_card_entry (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    name VARCHAR(255),
    description TEXT,
    value NUMERIC(12,2) NOT NULL,
    category_id UUID,
    user_id UUID,
    group_id UUID,
    tags TEXT[],
    observations TEXT,
    date DATE NOT NULL,
    confirmed BOOLEAN NOT NULL,
    bill_id UUID NOT NULL,
    installment_config_id UUID,
    installment INT,
    recurrence_config_id UUID,
    CONSTRAINT fk_cc_entry_category FOREIGN KEY (category_id) REFERENCES wallet_entry_category(id) ON DELETE SET NULL,
    CONSTRAINT fk_cc_entry_user FOREIGN KEY (user_id) REFERENCES "users"(id) ON DELETE SET NULL,
    CONSTRAINT fk_cc_entry_group FOREIGN KEY ("group_id") REFERENCES "group"(id) ON DELETE SET NULL,
    CONSTRAINT fk_cc_entry_bill FOREIGN KEY (bill_id) REFERENCES credit_card_bill(id) ON DELETE CASCADE,
    CONSTRAINT fk_cc_entry_installment FOREIGN KEY (installment_config_id) REFERENCES entry_installment_config(id),
    CONSTRAINT fk_cc_entry_recurrence FOREIGN KEY (recurrence_config_id) REFERENCES entry_recurrence_config(id)
);

CREATE INDEX idx_credit_card_entry_user ON credit_card_entry(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_credit_card_entry_group ON credit_card_entry(group_id) WHERE group_id IS NOT NULL;
CREATE INDEX idx_credit_card_entry_user_date ON credit_card_entry(user_id, date DESC) WHERE user_id IS NOT NULL;
CREATE INDEX idx_credit_card_entry_group_date ON credit_card_entry(group_id, date DESC) WHERE group_id IS NOT NULL;
CREATE INDEX idx_credit_card_entry_bill ON credit_card_entry(bill_id);
