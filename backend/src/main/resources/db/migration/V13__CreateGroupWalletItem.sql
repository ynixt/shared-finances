CREATE TABLE group_wallet_item
(
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL,
    wallet_item_id  UUID NOT NULL,

    CONSTRAINT fk_group_wallet_item_group
        FOREIGN KEY (group_id) REFERENCES "group" (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_group_wallet_item_wallet_item
        FOREIGN KEY (wallet_item_id) REFERENCES "wallet_item" (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_group_wallet_item_wallet_item ON group_wallet_item (wallet_item_id);
CREATE UNIQUE INDEX idx_group_wallet_item_group_id_wallet_item ON group_wallet_item (group_id, wallet_item_id);