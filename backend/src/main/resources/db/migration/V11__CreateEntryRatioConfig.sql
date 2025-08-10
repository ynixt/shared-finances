CREATE TABLE entry_ratio_config (
    id TEXT PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    entry_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    ratio DOUBLE PRECISION NOT NULL,
    paid BOOLEAN NOT NULL,
    CONSTRAINT fk_entry_ratio_user FOREIGN KEY (user_id) REFERENCES "users"(id)
    -- entry_id can refer to either debit_entry or credit_card_entry; not enforcing FK here
);

CREATE INDEX idx_entry_ratio_user ON entry_ratio_config(user_id);
CREATE INDEX idx_entry_ratio_entry ON entry_ratio_config(entry_id);
