CREATE TABLE debit_entry (
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
    bank_account_id UUID,
    recurrence_config_id UUID,
    CONSTRAINT fk_debit_entry_category FOREIGN KEY (category_id) REFERENCES wallet_entry_category(id) ON DELETE SET NULL,
    CONSTRAINT fk_debit_entry_user FOREIGN KEY (user_id) REFERENCES "users"(id) ON DELETE SET NULL,
    CONSTRAINT fk_debit_entry_group FOREIGN KEY ("group_id") REFERENCES "group"(id) ON DELETE SET NULL,
    CONSTRAINT fk_debit_entry_account FOREIGN KEY (bank_account_id) REFERENCES bank_account(id) ON DELETE SET NULL,
    CONSTRAINT fk_debit_entry_recurrence FOREIGN KEY (recurrence_config_id) REFERENCES entry_recurrence_config(id) ON DELETE SET NULL
);

CREATE INDEX idx_debit_entry_user ON debit_entry(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_debit_entry_group ON debit_entry(group_id) WHERE group_id IS NOT NULL;
CREATE INDEX idx_debit_entry_user_date ON debit_entry(user_id, date DESC) WHERE user_id IS NOT NULL;
CREATE INDEX idx_debit_entry_group_date ON debit_entry(group_id, date DESC) WHERE group_id IS NOT NULL;
CREATE INDEX idx_debit_entry_account ON debit_entry(bank_account_id);
