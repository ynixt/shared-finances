ALTER TABLE wallet_event
    RENAME COLUMN user_id TO created_by_user_id;

ALTER TABLE recurrence_event
    RENAME COLUMN user_id TO created_by_user_id;

UPDATE wallet_event we
SET created_by_user_id = (
    SELECT wi.user_id
    FROM wallet_entry wen
    INNER JOIN wallet_item wi ON wi.id = wen.wallet_item_id
    WHERE wen.wallet_event_id = we.id
    ORDER BY wen.created_at, wen.id
    LIMIT 1
)
WHERE we.created_by_user_id IS NULL;

UPDATE recurrence_event re
SET created_by_user_id = (
    SELECT wi.user_id
    FROM recurrence_entry ren
    INNER JOIN wallet_item wi ON wi.id = ren.wallet_item_id
    WHERE ren.wallet_event_id = re.id
    ORDER BY ren.created_at, ren.id
    LIMIT 1
)
WHERE re.created_by_user_id IS NULL;

UPDATE wallet_event we
SET created_by_user_id = fallback.id
FROM (
    SELECT id
    FROM "users"
    ORDER BY created_at, id
    LIMIT 1
) fallback
WHERE we.created_by_user_id IS NULL;

UPDATE recurrence_event re
SET created_by_user_id = fallback.id
FROM (
    SELECT id
    FROM "users"
    ORDER BY created_at, id
    LIMIT 1
) fallback
WHERE re.created_by_user_id IS NULL;

ALTER TABLE wallet_event
    ALTER COLUMN created_by_user_id SET NOT NULL;

ALTER TABLE recurrence_event
    ALTER COLUMN created_by_user_id SET NOT NULL;

ALTER TABLE wallet_event
    DROP CONSTRAINT IF EXISTS fk_w_entry_user;

ALTER TABLE recurrence_event
    DROP CONSTRAINT IF EXISTS fk_entry_rec_cfg_user;

ALTER TABLE wallet_event
    ADD CONSTRAINT fk_wallet_event_created_by_user
        FOREIGN KEY (created_by_user_id) REFERENCES "users"(id) ON DELETE CASCADE;

ALTER TABLE recurrence_event
    ADD CONSTRAINT fk_recurrence_event_created_by_user
        FOREIGN KEY (created_by_user_id) REFERENCES "users"(id) ON DELETE CASCADE;

DROP INDEX IF EXISTS idx_wallet_event_user_date_id;

CREATE INDEX idx_wallet_event_created_by_user_date_id
    ON wallet_event(created_by_user_id, date DESC, id DESC);

DROP INDEX IF EXISTS idx_recurrence_event_user;

CREATE INDEX idx_recurrence_event_created_by_user
    ON recurrence_event(created_by_user_id, next_execution, end_execution);

CREATE INDEX IF NOT EXISTS idx_wallet_entry_wallet_item_wallet_event_id
    ON wallet_entry(wallet_item_id, wallet_event_id);

CREATE INDEX IF NOT EXISTS idx_recurrence_entry_wallet_item_wallet_event_id
    ON recurrence_entry(wallet_item_id, wallet_event_id);
