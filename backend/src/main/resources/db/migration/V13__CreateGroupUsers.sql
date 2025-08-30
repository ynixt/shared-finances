CREATE TABLE group_users
(
    group_id UUID NOT NULL,
    user_id  UUID NOT NULL,

    PRIMARY KEY (group_id, user_id),

    CONSTRAINT fk_group_users_group
        FOREIGN KEY (group_id) REFERENCES "group" (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_group_users_user
        FOREIGN KEY (user_id) REFERENCES "users" (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_group_users_user_id ON group_users (user_id);
