CREATE UNIQUE INDEX idx_group_has_users on "group_has_users" (group_id, user_id);
CREATE INDEX idx_group_has_users_group_id on "group_has_users" (group_id);
CREATE INDEX idx_group_has_users_user_id on "group_has_users" (user_id);
