ALTER TABLE import_batch
    ADD COLUMN status TEXT NOT NULL DEFAULT 'COMPLETED',
    ADD COLUMN request_payload TEXT,
    ADD COLUMN error_message TEXT,
    ADD COLUMN lease_expires_at TIMESTAMPTZ,
    ADD COLUMN worker_id TEXT,
    ADD COLUMN started_at TIMESTAMPTZ,
    ADD COLUMN finished_at TIMESTAMPTZ,
    ADD COLUMN retries INT NOT NULL DEFAULT 0;

ALTER TABLE import_batch
    ADD CONSTRAINT ck_import_batch_status
        CHECK (status IN ('QUEUED', 'RUNNING', 'COMPLETED', 'FAILED')),
    ADD CONSTRAINT ck_import_batch_retries
        CHECK (retries >= 0);

UPDATE import_batch
SET
    status = 'COMPLETED',
    started_at = COALESCE(created_at, NOW()),
    finished_at = COALESCE(updated_at, created_at, NOW())
WHERE status = 'COMPLETED';

CREATE UNIQUE INDEX uq_import_batch_active_user_hash
    ON import_batch(user_id, file_hash)
    WHERE status IN ('QUEUED', 'RUNNING');

CREATE UNIQUE INDEX uq_import_batch_running_user
    ON import_batch(user_id)
    WHERE status = 'RUNNING';

CREATE INDEX idx_import_batch_status_created
    ON import_batch(status, created_at, id);

CREATE INDEX idx_import_batch_running_lease
    ON import_batch(lease_expires_at)
    WHERE status = 'RUNNING' AND lease_expires_at IS NOT NULL;
