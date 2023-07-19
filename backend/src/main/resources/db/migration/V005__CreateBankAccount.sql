create table "bank_account"
(
    id               bigint generated by default as identity primary key not null,
    created_at       timestamp with time zone                            not null,
    updated_at       timestamp with time zone,
    user_id          bigint                                              not null,
    name             text                                                not null,
    balance          numeric(12, 2)                                      not null,
    enabled          boolean                                             not null default true,
    display_on_group boolean                                             not null,
    CONSTRAINT fk_bank_account_user_id
        FOREIGN KEY (user_id)
            REFERENCES "user" (id)
);

CREATE INDEX idx_bank_account_user_id on "bank_account" (user_id);