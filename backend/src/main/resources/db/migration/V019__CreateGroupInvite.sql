create table "group_invite"
(
    id         bigint generated by default as identity primary key not null,
    created_at timestamp with time zone                            not null,
    updated_at timestamp with time zone,
    group_id   bigint                                              not null,
    code       uuid                                                not null,
    expires_on timestamp with time zone                            not null,
    CONSTRAINT fk_group_invite_group_id
        FOREIGN KEY (group_id)
            REFERENCES "group" (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX idx_group_invite on "group_invite" (group_id, code, expires_on);
CREATE UNIQUE INDEX idx_group_invite_group_id_code on "group_invite" (group_id, code);
CREATE UNIQUE INDEX idx_group_invite_code on "group_invite" (code);
CREATE INDEX idx_group_invite_expires_on on "group_invite" (expires_on);
