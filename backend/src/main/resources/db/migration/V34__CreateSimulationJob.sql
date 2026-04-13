CREATE TABLE simulation_job (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    owner_user_id UUID NOT NULL REFERENCES "users"(id) ON DELETE CASCADE,
    type TEXT NOT NULL,
    status TEXT NOT NULL,
    request_payload TEXT,
    result_payload TEXT,
    error_message TEXT,
    lease_expires_at TIMESTAMPTZ,
    worker_id TEXT,
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    retries INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_simulation_job_owner_status_created
    ON simulation_job(owner_user_id, status, created_at, id);

CREATE INDEX idx_simulation_job_status_created
    ON simulation_job(status, created_at, id);

CREATE INDEX idx_simulation_job_running_lease
    ON simulation_job(lease_expires_at)
    WHERE status = 'RUNNING' AND lease_expires_at IS NOT NULL;

CREATE INDEX idx_simulation_job_created_at
    ON simulation_job(created_at);

CREATE UNIQUE INDEX uq_simulation_job_running_owner
    ON simulation_job(owner_user_id)
    WHERE status = 'RUNNING';
