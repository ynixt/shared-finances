alter table "transaction"
    drop constraint fk_transaction_second_bank_account_id;
alter table "transaction"
    drop constraint fk_transaction_second_user_id;
drop index idx_transaction_second_bank_account_id;
drop index idx_transaction_second_user_id;

alter table "transaction"
    drop column "second_bank_account_id";

alter table "transaction"
    drop column "second_user_id";

alter table "transaction"
    add column "other_side_id" bigint;

ALTER TABLE "transaction"
    ADD CONSTRAINT fk_transaction_other_side_id FOREIGN KEY (other_side_id) REFERENCES "transaction" ON DELETE CASCADE;

CREATE INDEX idx_transaction_other_side_id on "transaction" (other_side_id);
