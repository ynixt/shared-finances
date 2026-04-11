ALTER TABLE wallet_entry
    ADD COLUMN contribution_percent NUMERIC(5, 2);

ALTER TABLE recurrence_entry
    ADD COLUMN contribution_percent NUMERIC(5, 2);
