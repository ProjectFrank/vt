CREATE EXTENSION pgcrypto;
CREATE TABLE users (
        id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
        username text NOT NULL UNIQUE,
        password_hash text NOT NULL
        );
