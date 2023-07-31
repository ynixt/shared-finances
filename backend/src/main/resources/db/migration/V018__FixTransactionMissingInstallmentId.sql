alter table "transaction"
    add column "installment_id" text;

CREATE INDEX idx_transaction_installment_id on "transaction" (installment_id) where installment_id != null;
