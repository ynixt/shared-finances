CREATE TABLE recurrence_event (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    type TEXT NOT NULL,
    payment_type TEXT NOT NULL,
    periodicity TEXT NOT NULL,
    name CITEXT CHECK (char_length(name) <= 255),
    description TEXT,
    category_id UUID,
    user_id UUID,
    group_id UUID,
    tags TEXT[],
    observations TEXT,
    qty_Executed INT NOT NULL,
    qty_limit INT,
    last_execution DATE,
    next_execution DATE,
    end_execution DATE,
    CONSTRAINT fk_entry_rec_cfg_category FOREIGN KEY (category_id) REFERENCES wallet_entry_category(id) ON DELETE SET NULL,
    CONSTRAINT fk_entry_rec_cfg_user FOREIGN KEY (user_id) REFERENCES "users"(id) ON DELETE SET NULL,
    CONSTRAINT fk_entry_rec_cfg_group FOREIGN KEY ("group_id") REFERENCES "group"(id) ON DELETE SET NULL
);

CREATE INDEX idx_recurrence_event_user ON recurrence_event(user_id, next_execution, end_execution) WHERE user_id IS NOT NULL;
CREATE INDEX idx_recurrence_event_group ON recurrence_event(group_id, next_execution, end_execution)  WHERE group_id IS NOT NULL;
CREATE INDEX idx_recurrence_event_next_exec ON recurrence_event(next_execution) WHERE next_execution IS NOT NULL;
