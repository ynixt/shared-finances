ALTER TABLE wallet_event
    ADD COLUMN transfer_purpose VARCHAR(32) NOT NULL DEFAULT 'GENERAL';

ALTER TABLE recurrence_event
    ADD COLUMN transfer_purpose VARCHAR(32) NOT NULL DEFAULT 'GENERAL';
