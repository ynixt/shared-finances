CREATE TABLE group_invite (
    group_id UUID NOT NULL,
    expire_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_group_invite_group FOREIGN KEY (group_id) REFERENCES "group"(id) ON DELETE CASCADE
);

CREATE INDEX idx_group_invite_group ON group_invite(group_id);
CREATE INDEX idx_group_invite_expire ON group_invite(expire_at);
