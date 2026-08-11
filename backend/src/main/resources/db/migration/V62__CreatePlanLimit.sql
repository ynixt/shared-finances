CREATE TABLE plan_limit (
    scope TEXT NOT NULL,
    plan_key TEXT NOT NULL,
    limit_key TEXT NOT NULL,
    limit_value INT,
    PRIMARY KEY (scope, plan_key, limit_key),
    CONSTRAINT plan_limit_value_non_negative CHECK (limit_value IS NULL OR limit_value >= 0)
);

INSERT INTO plan_limit(scope, plan_key, limit_key, limit_value) VALUES
    ('USER', 'USER', 'BANK_ACCOUNTS', 10),
    ('USER', 'USER', 'CREDIT_CARDS', 10),
    ('USER', 'USER', 'CATEGORIES', 50),
    ('USER', 'USER', 'GOALS', 10),
    ('USER', 'USER', 'ACTIVE_SCHEDULES', 50),
    ('USER', 'USER', 'IMPORTS_PER_MONTH', 2),
    ('USER', 'USER', 'SIMULATIONS_PER_MONTH', 2),
    ('USER', 'USER', 'OWNED_GROUPS', 2),
    ('USER', 'PRO', 'BANK_ACCOUNTS', 1000),
    ('USER', 'PRO', 'CREDIT_CARDS', 1000),
    ('USER', 'PRO', 'CATEGORIES', 1000),
    ('USER', 'PRO', 'GOALS', 1000),
    ('USER', 'PRO', 'ACTIVE_SCHEDULES', 1000),
    ('USER', 'PRO', 'IMPORTS_PER_MONTH', 1000),
    ('USER', 'PRO', 'SIMULATIONS_PER_MONTH', 1000),
    ('USER', 'PRO', 'OWNED_GROUPS', 100);

CREATE INDEX idx_plan_quota_wallet_item_user_type ON wallet_item(user_id, type);
CREATE INDEX idx_plan_quota_category_user ON wallet_entry_category(user_id) WHERE user_id IS NOT NULL AND group_id IS NULL;
CREATE INDEX idx_plan_quota_goal_user ON financial_goal(user_id) WHERE user_id IS NOT NULL AND group_id IS NULL;
CREATE INDEX idx_plan_quota_active_schedule_user ON recurrence_event(created_by_user_id) WHERE group_id IS NULL AND next_execution IS NOT NULL;
