CREATE TABLE tracked_repos (
        id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
        user_id uuid REFERENCES users (id),
        last_seen timestamptz,
        github_id text NOT NULL,
        UNIQUE (user_id, github_id)
        )
