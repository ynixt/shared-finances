CREATE TABLE entry_recurrence_config (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    type TEXT NOT NULL,
    payment_type TEXT NOT NULL,
    periodicity TEXT NOT NULL,
    name CITEXT CHECK (char_length(name) <= 255),
    description TEXT,
    value NUMERIC(12,2) NOT NULL,
    category_id UUID,
    user_id UUID,
    group_id UUID,
    origin_id UUID NOT NULL,
    target_id UUID,
    tags TEXT[],
    observations TEXT,
    qty_Executed INT NOT NULL,
    qty_limit INT,
    last_execution DATE NOT NULL,
    next_execution DATE,
    CONSTRAINT fk_entry_rec_cfg_category FOREIGN KEY (category_id) REFERENCES wallet_entry_category(id) ON DELETE SET NULL,
    CONSTRAINT fk_entry_rec_cfg_user FOREIGN KEY (user_id) REFERENCES "users"(id) ON DELETE SET NULL,
    CONSTRAINT fk_entry_rec_cfg_group FOREIGN KEY ("group_id") REFERENCES "group"(id) ON DELETE SET NULL,
    CONSTRAINT fk_w_origin FOREIGN KEY (origin_id) REFERENCES wallet_item(id) ON DELETE SET NULL,
    CONSTRAINT fk_w_target FOREIGN KEY (target_id) REFERENCES wallet_item(id) ON DELETE SET NULL
);

CREATE INDEX idx_entry_recurrence_config_user ON entry_recurrence_config(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_entry_recurrence_config_group ON entry_recurrence_config(group_id)  WHERE group_id IS NOT NULL;
CREATE INDEX idx_entry_recurrence_config_next_exec ON entry_recurrence_config(next_execution) WHERE next_execution IS NOT NULL;
