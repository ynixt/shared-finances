INSERT INTO plan_limit(scope, plan_key, limit_key, limit_value) VALUES
    ('USER', 'USER', 'EXPORTS_PER_MONTH', 10),
    ('USER', 'PRO', 'EXPORTS_PER_MONTH', 1000),
    ('USER', 'USER', 'EXPORT_MAX_LINES', 2000),
    ('USER', 'PRO', 'EXPORT_MAX_LINES', 50000);
