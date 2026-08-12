CREATE TABLE export_batch (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    user_id UUID NOT NULL REFERENCES "users"(id) ON DELETE CASCADE,
    status TEXT NOT NULL DEFAULT 'QUEUED',
    format TEXT NOT NULL,
    filter_payload TEXT NOT NULL,
    row_count INT CHECK (row_count IS NULL OR row_count >= 0),
    counted_at TIMESTAMPTZ,
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    first_downloaded_at TIMESTAMPTZ,
    file_key TEXT,
    file_deleted_at TIMESTAMPTZ,
    error_message TEXT,
    lease_expires_at TIMESTAMPTZ,
    worker_id TEXT,
    retries INT NOT NULL DEFAULT 0,
    CONSTRAINT ck_export_batch_status
        CHECK (status IN ('QUEUED', 'RUNNING', 'COMPLETED', 'FAILED', 'EXPIRED')),
    CONSTRAINT ck_export_batch_format CHECK (format IN ('CSV', 'XLSX')),
    CONSTRAINT ck_export_batch_retries CHECK (retries >= 0)
);

CREATE UNIQUE INDEX uq_export_batch_running_user
    ON export_batch(user_id)
    WHERE status = 'RUNNING';

CREATE INDEX idx_export_batch_user_created
    ON export_batch(user_id, created_at DESC, id DESC);

CREATE INDEX idx_export_batch_status_created
    ON export_batch(status, created_at, id);

CREATE INDEX idx_export_batch_running_lease
    ON export_batch(lease_expires_at)
    WHERE status = 'RUNNING' AND lease_expires_at IS NOT NULL;

CREATE INDEX idx_export_batch_download_purge
    ON export_batch(first_downloaded_at)
    WHERE status = 'COMPLETED' AND file_deleted_at IS NULL AND first_downloaded_at IS NOT NULL;

CREATE INDEX idx_export_batch_age_purge
    ON export_batch(finished_at)
    WHERE status = 'COMPLETED' AND file_deleted_at IS NULL AND finished_at IS NOT NULL;

CREATE INDEX idx_export_batch_user_counted_at
    ON export_batch(user_id, counted_at)
    WHERE counted_at IS NOT NULL;
