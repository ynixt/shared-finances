ALTER TABLE "group"
    ADD COLUMN owner_user_id UUID;

UPDATE "group" g
SET owner_user_id = (
    SELECT gu.user_id
    FROM group_user gu
    WHERE gu.group_id = g.id
      AND gu.role = 'ADMIN'
    ORDER BY gu.id
    LIMIT 1
);

UPDATE "group" g
SET owner_user_id = (
    SELECT gu.user_id
    FROM group_user gu
    WHERE gu.group_id = g.id
    ORDER BY gu.id
    LIMIT 1
)
WHERE g.owner_user_id IS NULL;

DELETE FROM "group"
WHERE owner_user_id IS NULL;

ALTER TABLE "group"
    ALTER COLUMN owner_user_id SET NOT NULL,
    ADD CONSTRAINT fk_group_owner_user
        FOREIGN KEY (owner_user_id) REFERENCES "users" (id)
            ON DELETE RESTRICT;

CREATE INDEX idx_group_owner_user_id ON "group" (owner_user_id);
