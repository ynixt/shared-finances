CREATE TABLE wallet_event (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    type TEXT NOT NULL,
    payment_type TEXT NOT NULL,
    name CITEXT CHECK (char_length(name) <= 255),
    description TEXT,
    category_id UUID,
    user_id UUID,
    group_id UUID,
    tags TEXT[],
    observations TEXT,
    date DATE NOT NULL,
    confirmed BOOLEAN NOT NULL,
    installment INT,
    recurrence_event_id UUID,
    CONSTRAINT fk_w_entry_category FOREIGN KEY (category_id) REFERENCES wallet_entry_category(id) ON DELETE SET NULL,
    CONSTRAINT fk_w_entry_user FOREIGN KEY (user_id) REFERENCES "users"(id) ON DELETE SET NULL,
    CONSTRAINT fk_w_entry_group FOREIGN KEY ("group_id") REFERENCES "group"(id) ON DELETE SET NULL,
    CONSTRAINT fk_w_entry_recurrence FOREIGN KEY (recurrence_event_id) REFERENCES recurrence_event(id)
);

CREATE INDEX idx_wallet_event_user_date_id ON wallet_event(user_id, date DESC, id DESC) WHERE user_id IS NOT NULL;
CREATE INDEX idx_wallet_event_group_date_id ON wallet_event(group_id, date DESC, id DESC) WHERE group_id IS NOT NULL;
CREATE INDEX idx_wallet_event_tags ON wallet_event USING GIN(tags);
