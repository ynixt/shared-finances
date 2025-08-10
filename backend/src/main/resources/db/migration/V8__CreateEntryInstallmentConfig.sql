CREATE TABLE entry_installment_config (
    id TEXT PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    name VARCHAR(255),
    description TEXT,
    value NUMERIC(12,2) NOT NULL,
    category_id TEXT,
    user_id TEXT,
    group_id TEXT,
    tags TEXT[],
    observations TEXT NOT NULL,
    installments INT NOT NULL,
    last_execution DATE NOT NULL,
    next_execution DATE NOT NULL,
    CONSTRAINT fk_entry_ins_cfg_category FOREIGN KEY (category_id) REFERENCES wallet_entry_category(id),
    CONSTRAINT fk_entry_ins_cfg_user FOREIGN KEY (user_id) REFERENCES "users"(id),
    CONSTRAINT fk_entry_ins_cfg_group FOREIGN KEY ("group_id") REFERENCES "group"(id)
);

CREATE INDEX idx_entry_installment_config_user ON entry_installment_config(user_id);
CREATE INDEX idx_entry_installment_config_group ON entry_installment_config(group_id);
CREATE INDEX idx_entry_installment_config_next_exec ON entry_installment_config(next_execution);
