CREATE TABLE group_bank_account
(
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL,
    bank_account_id  UUID NOT NULL,

    CONSTRAINT fk_group_bank_account_group
        FOREIGN KEY (group_id) REFERENCES "group" (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_group_bank_account_bank_account
        FOREIGN KEY (bank_account_id) REFERENCES "bank_account" (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_group_bank_account_bank_account ON group_bank_account (bank_account_id);
CREATE UNIQUE INDEX idx_group_bank_account_group_id_bank_account ON group_bank_account (group_id, bank_account_id);