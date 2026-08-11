ALTER TABLE import_batch ADD COLUMN counted_at TIMESTAMPTZ;
ALTER TABLE simulation_job ADD COLUMN counted_at TIMESTAMPTZ;
ALTER TABLE import_batch ADD COLUMN deleted_at TIMESTAMPTZ;
ALTER TABLE simulation_job ADD COLUMN deleted_at TIMESTAMPTZ;

ALTER TABLE import_batch DROP CONSTRAINT ck_import_batch_status;
ALTER TABLE import_batch
    ADD CONSTRAINT ck_import_batch_status
        CHECK (status IN (
            'QUEUED', 'RUNNING', 'COMPLETED', 'FAILED',
            'UNDO_QUEUED', 'UNDO_RUNNING', 'UNDO_FAILED', 'UNDONE'
        ));

UPDATE import_batch
SET counted_at = finished_at
WHERE status IN ('COMPLETED', 'UNDO_QUEUED', 'UNDO_RUNNING', 'UNDO_FAILED')
  AND finished_at IS NOT NULL;

UPDATE simulation_job
SET counted_at = finished_at
WHERE status = 'COMPLETED'
  AND finished_at IS NOT NULL;

CREATE INDEX idx_import_batch_user_counted_at
    ON import_batch(user_id, counted_at)
    WHERE counted_at IS NOT NULL;

CREATE INDEX idx_simulation_job_requester_counted_at
    ON simulation_job(requested_by_user_id, counted_at)
    WHERE counted_at IS NOT NULL;
