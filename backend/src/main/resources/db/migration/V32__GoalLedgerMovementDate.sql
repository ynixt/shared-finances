ALTER TABLE financial_goal_ledger_movement
    ADD COLUMN movement_date DATE;

UPDATE financial_goal_ledger_movement
SET movement_date = (created_at AT TIME ZONE 'UTC')::date;

ALTER TABLE financial_goal_ledger_movement
    ALTER COLUMN movement_date SET NOT NULL;

CREATE INDEX idx_fin_goal_ledger_goal_movement_date ON financial_goal_ledger_movement (financial_goal_id, movement_date);
