CREATE TABLE import_batch (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    user_id UUID NOT NULL REFERENCES "users"(id) ON DELETE CASCADE,
    file_hash VARCHAR(64) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    format VARCHAR(16) NOT NULL,
    wallet_item_id UUID NOT NULL REFERENCES wallet_item(id) ON DELETE CASCADE,
    qty INT NOT NULL CHECK (qty >= 0),
    total_credit NUMERIC(19, 2) NOT NULL CHECK (total_credit >= 0),
    total_debit NUMERIC(19, 2) NOT NULL CHECK (total_debit >= 0)
);

CREATE INDEX idx_import_batch_user_created
    ON import_batch(user_id, created_at DESC, id DESC);

CREATE INDEX idx_import_batch_user_hash
    ON import_batch(user_id, file_hash);

ALTER TABLE wallet_event
    ADD COLUMN import_batch_id UUID REFERENCES import_batch(id) ON DELETE SET NULL;

ALTER TABLE recurrence_event
    ADD COLUMN import_batch_id UUID REFERENCES import_batch(id) ON DELETE SET NULL;

CREATE INDEX idx_wallet_event_import_batch
    ON wallet_event(import_batch_id)
    WHERE import_batch_id IS NOT NULL;

CREATE INDEX idx_recurrence_event_import_batch
    ON recurrence_event(import_batch_id)
    WHERE import_batch_id IS NOT NULL;
