CREATE TABLE entry_recurrence_config (
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
    observations TEXT,
    start_date DATE NOT NULL,
    end_date DATE,
    type TEXT NOT NULL,
    recurrence_qty INT NOT NULL,
    last_execution DATE NOT NULL,
    next_execution DATE NOT NULL,
    CONSTRAINT fk_entry_rec_cfg_category FOREIGN KEY (category_id) REFERENCES wallet_entry_category(id) ON DELETE SET NULL,
    CONSTRAINT fk_entry_rec_cfg_user FOREIGN KEY (user_id) REFERENCES "users"(id) ON DELETE SET NULL,
    CONSTRAINT fk_entry_rec_cfg_group FOREIGN KEY ("group_id") REFERENCES "group"(id) ON DELETE SET NULL
);

CREATE INDEX idx_entry_recurrence_config_user ON entry_recurrence_config(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_entry_recurrence_config_group ON entry_recurrence_config(group_id)  WHERE group_id IS NOT NULL;
CREATE INDEX idx_entry_recurrence_config_next_exec ON entry_recurrence_config(next_execution);
