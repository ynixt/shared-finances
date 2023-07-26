alter table "transaction"
    drop column "totalinstallments";

alter table "transaction"
    add column "total_installments" int;

alter table "transaction"
    add column "second_bank_account_id" bigint;

alter table "transaction"
    add column "second_user_id" bigint;

ALTER TABLE "transaction"
    ADD CONSTRAINT fk_transaction_second_bank_account_id FOREIGN KEY (second_bank_account_id) REFERENCES "bank_account" ON DELETE CASCADE;

ALTER TABLE "transaction"
    ADD CONSTRAINT fk_transaction_second_user_id FOREIGN KEY (second_user_id) REFERENCES "bank_account" ON DELETE CASCADE;

CREATE INDEX idx_transaction_second_bank_account_id on "transaction" (second_bank_account_id);
CREATE INDEX idx_transaction_second_user_id on "transaction" (second_user_id);
