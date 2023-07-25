alter table "transaction"
    add column "group_id" bigint;

ALTER TABLE "transaction"
    ADD CONSTRAINT fk_transaction_group_id FOREIGN KEY (group_id) REFERENCES "group" ON DELETE CASCADE;

CREATE INDEX idx_transaction_group_id on "transaction" (group_id);
