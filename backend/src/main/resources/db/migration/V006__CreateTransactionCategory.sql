create table "transaction_category"
(
    id         bigint generated by default as identity primary key not null,
    created_at timestamp with time zone                            not null,
    updated_at timestamp with time zone,
    user_id    bigint,
    bank_id    bigint,
    name       text                                                not null,
    color      text                                                not null,
    CONSTRAINT fk_transaction_category_user_id
        FOREIGN KEY (user_id)
            REFERENCES "user" (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX idx_transaction_category_user_id on "transaction_category" (user_id);
