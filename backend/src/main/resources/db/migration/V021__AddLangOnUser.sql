alter table "user"
    add column "lang" text;

update "user"
set "lang" = 'en';

alter table "user"
    alter column "lang" set not null;
