ALTER TABLE financial_goal_contribution_schedule
    ADD COLUMN removes_allocation BOOLEAN NOT NULL DEFAULT FALSE;
