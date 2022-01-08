ALTER TABLE users
-- NOT NULL constraint without default only acceptable if users table is empty
ADD COLUMN encrypted_github_token bytea NOT NULL;
