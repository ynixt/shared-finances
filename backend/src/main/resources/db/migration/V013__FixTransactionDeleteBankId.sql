alter table "transaction_category"
    drop column bank_id;

alter table "transaction_category"
    add column "group_id" bigint;

ALTER TABLE "transaction_category"
    ADD CONSTRAINT fk_transaction_category_group_id FOREIGN KEY (group_id) REFERENCES "group" ON DELETE CASCADE;

CREATE INDEX idx_transaction_category on "transaction_category" (group_id);
