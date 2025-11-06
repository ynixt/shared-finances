CREATE TABLE group_credit_card
(
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL,
    credit_card_id  UUID NOT NULL,

    CONSTRAINT fk_group_credit_card_group
        FOREIGN KEY (group_id) REFERENCES "group" (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_group_credit_card_credit_card
        FOREIGN KEY (credit_card_id) REFERENCES "credit_card" (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_group_credit_card_credit_card ON group_credit_card (credit_card_id);
CREATE UNIQUE INDEX idx_group_credit_card_group_id_credit_card ON group_credit_card (group_id, credit_card_id);