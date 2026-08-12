-- Remove indexes that are fully covered by another index on the same table.
-- Each pair below was introduced when the plan-quota indexes (V62/V64) duplicated
-- indexes that already existed since the original table migrations.

-- Identical to idx_financial_goal_group_id (V31).
DROP INDEX IF EXISTS idx_plan_quota_goal_group;

-- Identical to idx_wallet_entry_category_group (V3).
DROP INDEX IF EXISTS idx_plan_quota_category_group;

-- Covered by idx_plan_quota_unexpired_invite_group (group_id, expire_at) from V64.
DROP INDEX IF EXISTS idx_group_invite_group;

-- Covered by idx_plan_quota_wallet_item_user_type (user_id, type) from V62.
DROP INDEX IF EXISTS idx_wallet_item_user_id;
