ALTER TABLE users ADD COLUMN last_login_at TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN inactivity_notice_stage INT;

UPDATE users SET last_login_at = CURRENT_TIMESTAMP;

ALTER TABLE users ALTER COLUMN last_login_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE users ALTER COLUMN last_login_at SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT users_inactivity_notice_stage_check
    CHECK (inactivity_notice_stage IN (30, 7, 1));

CREATE INDEX idx_users_last_login_at ON users(last_login_at);
