ALTER TABLE users ADD COLUMN role TEXT;

UPDATE users
SET role = UPPER('${default-role}');

ALTER TABLE users ALTER COLUMN role SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('USER', 'PRO', 'ADMINISTRATOR'));
