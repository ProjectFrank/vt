-- :name create-user* :! :n
-- :doc Create a user
INSERT INTO users (username, password_hash)
VALUES (:username, :password-hash)

-- :name count-users* :? :1
-- :doc Count users with a given username
SELECT COUNT(*) as count
FROM users
WHERE username=:username

-- :name find-user* :? :1
-- :doc Find user with given username
SELECT id, password_hash
FROM users
WHERE username=:username
