ALTER TABLE simulation_job
    ADD COLUMN requested_by_user_id UUID REFERENCES "users"(id) ON DELETE CASCADE;

UPDATE simulation_job
SET requested_by_user_id = owner_user_id
WHERE requested_by_user_id IS NULL;

ALTER TABLE simulation_job
    ALTER COLUMN requested_by_user_id SET NOT NULL;

ALTER TABLE simulation_job
    ALTER COLUMN owner_user_id DROP NOT NULL;

ALTER TABLE simulation_job
    ADD COLUMN owner_group_id UUID REFERENCES "group"(id) ON DELETE CASCADE;

ALTER TABLE simulation_job
    ADD CONSTRAINT chk_simulation_job_owner_scope
        CHECK (
            (owner_user_id IS NOT NULL AND owner_group_id IS NULL) OR
            (owner_user_id IS NULL AND owner_group_id IS NOT NULL)
        );

CREATE INDEX idx_simulation_job_owner_group_status_created
    ON simulation_job(owner_group_id, status, created_at, id)
    WHERE owner_group_id IS NOT NULL;

CREATE UNIQUE INDEX uq_simulation_job_running_owner_group
    ON simulation_job(owner_group_id)
    WHERE status = 'RUNNING' AND owner_group_id IS NOT NULL;
