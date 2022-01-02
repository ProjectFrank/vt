-- :name create-user* :! :n
-- :doc Create a user
INSERT INTO users (username, password_hash)
VALUES (:username, :password-hash)

-- :name count-users* :1 :n
-- :doc Count users with a given username
SELECT COUNT(*) as count
FROM users
WHERE username=:username
