alter table transaction_category
    add column type text not null;

CREATE INDEX idx_transaction_category_type on "transaction_category" (type);
CREATE INDEX idx_transaction_category_type_user_id on "transaction_category" (type, user_id);