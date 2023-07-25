ALTER TABLE "transaction_category"
    ADD CONSTRAINT c_transaction_category_name_uq UNIQUE NULLS NOT DISTINCT (name, user_id, group_id);

DROP INDEX idx_transaction_category_user_id;
CREATE INDEX idx_transaction_category_user_id on "transaction_category" (user_id);

