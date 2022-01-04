CREATE TABLE tracked_repos (
        id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
        user_id uuid REFERENCES users (id),
        last_seen_release timestamp,
        github_id text NOT NULL,
        UNIQUE (user_id, github_id)
        )
