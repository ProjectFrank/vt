-- :name create-user* :! :n
-- :doc Create a user
INSERT INTO users (username, password_hash, encrypted_github_token)
VALUES (:username, :password-hash, :encrypted-github-token)

-- :name count-users* :? :1
-- :doc Count users with a given username
SELECT COUNT(*) as count
FROM users
WHERE username=:username

-- :name find-user* :? :1
-- :doc Find user with given username
SELECT id, username, password_hash, encrypted_github_token
FROM users
WHERE username=:username

-- :name add-tracked-repo* :<! :n
INSERT INTO tracked_repos (user_id, github_id)
VALUES (:user-id, :github-id)
RETURNING id

-- :name count-tracked-repo-by-github-id* :? :1
SELECT COUNT(*) as count
FROM tracked_repos
WHERE github_id=:github-id AND user_id=:user-id

-- :name find-tracked-repo-github-ids* :? :*
SELECT github_id
FROM tracked_repos
WHERE user_id=:user-id

-- :name find-tracked-repo* :? :1
SELECT id
FROM tracked_repos
WHERE user_id = :user-id AND github_id = :github-id
