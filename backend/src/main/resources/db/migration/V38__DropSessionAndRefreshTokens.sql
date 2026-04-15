-- Auth sessions and refresh tokens are stored in Redis; relational tables are no longer used.
DROP TABLE IF EXISTS refresh_tokens;
DROP TABLE IF EXISTS session;
