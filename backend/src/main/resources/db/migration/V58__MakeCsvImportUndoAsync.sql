ALTER TABLE import_batch
    DROP CONSTRAINT ck_import_batch_status;

ALTER TABLE import_batch
    ADD CONSTRAINT ck_import_batch_status
        CHECK (status IN (
            'QUEUED',
            'RUNNING',
            'COMPLETED',
            'FAILED',
            'UNDO_QUEUED',
            'UNDO_RUNNING',
            'UNDO_FAILED'
        ));

DROP INDEX uq_import_batch_active_user_hash;
CREATE UNIQUE INDEX uq_import_batch_active_user_hash
    ON import_batch(user_id, file_hash)
    WHERE status IN ('QUEUED', 'RUNNING', 'UNDO_QUEUED', 'UNDO_RUNNING');

DROP INDEX uq_import_batch_running_user;
CREATE UNIQUE INDEX uq_import_batch_running_user
    ON import_batch(user_id)
    WHERE status IN ('RUNNING', 'UNDO_RUNNING');

DROP INDEX idx_import_batch_running_lease;
CREATE INDEX idx_import_batch_running_lease
    ON import_batch(lease_expires_at)
    WHERE status IN ('RUNNING', 'UNDO_RUNNING') AND lease_expires_at IS NOT NULL;
