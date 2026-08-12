CREATE TABLE plan_quota_monthly_usage
(
    user_id UUID NOT NULL REFERENCES "users"(id) ON DELETE CASCADE,
    quota VARCHAR(64) NOT NULL,
    month_start DATE NOT NULL,
    usage BIGINT NOT NULL CHECK (usage >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, quota)
);

INSERT INTO plan_quota_monthly_usage (user_id, quota, month_start, usage)
SELECT user_id,
       'EXPORTS_PER_MONTH',
       (date_trunc('month', counted_at AT TIME ZONE 'UTC'))::date,
       COUNT(*)
FROM export_batch
WHERE counted_at IS NOT NULL
  AND counted_at >= date_trunc('month', CURRENT_TIMESTAMP AT TIME ZONE 'UTC') AT TIME ZONE 'UTC'
GROUP BY user_id, (date_trunc('month', counted_at AT TIME ZONE 'UTC'))::date;

DELETE FROM export_batch WHERE status = 'EXPIRED';

DROP INDEX IF EXISTS idx_export_batch_user_counted_at;
