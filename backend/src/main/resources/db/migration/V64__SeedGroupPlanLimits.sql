INSERT INTO plan_limit(scope, plan_key, limit_key, limit_value) VALUES
    ('GROUP', 'COMMON', 'GROUP_CATEGORIES', 50),
    ('GROUP', 'COMMON', 'GROUP_GOALS', 10),
    ('GROUP', 'COMMON', 'GROUP_ACTIVE_SCHEDULES', 50),
    ('GROUP', 'COMMON', 'GROUP_MEMBERS', 4),
    ('GROUP', 'PRO', 'GROUP_CATEGORIES', 1000),
    ('GROUP', 'PRO', 'GROUP_GOALS', 1000),
    ('GROUP', 'PRO', 'GROUP_ACTIVE_SCHEDULES', 1000),
    ('GROUP', 'PRO', 'GROUP_MEMBERS', 100);

CREATE INDEX idx_plan_quota_category_group
    ON wallet_entry_category(group_id) WHERE group_id IS NOT NULL;
CREATE INDEX idx_plan_quota_goal_group
    ON financial_goal(group_id) WHERE group_id IS NOT NULL;
CREATE INDEX idx_plan_quota_active_schedule_group
    ON recurrence_event(group_id) WHERE group_id IS NOT NULL AND next_execution IS NOT NULL;
CREATE INDEX idx_plan_quota_unexpired_invite_group
    ON group_invite(group_id, expire_at);
