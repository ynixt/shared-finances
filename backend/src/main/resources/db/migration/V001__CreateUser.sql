create table "user"
(
    id         bigint generated by default as identity primary key not null,
    uid        text                                                not null,
    created_at timestamp with time zone                            not null,
    updated_at timestamp with time zone,
    email      text                                                not null,
    name       text                                                not null,
    photo_url  text
);

CREATE UNIQUE INDEX idx_user_uid on "user" (uid);
CREATE UNIQUE INDEX idx_user_email on "user" (email);