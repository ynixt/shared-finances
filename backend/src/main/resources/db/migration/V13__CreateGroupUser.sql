CREATE TABLE group_user
(
    group_id UUID NOT NULL,
    user_id  UUID NOT NULL,
    role     TEXT NOT NULL,

    PRIMARY KEY (group_id, user_id),

    CONSTRAINT fk_group_user_group
        FOREIGN KEY (group_id) REFERENCES "group" (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_group_user_user
        FOREIGN KEY (user_id) REFERENCES "users" (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_group_user_user_id ON group_user (user_id);
CREATE INDEX idx_group_user_group_id_role ON group_user (group_id, role);