(ns version-tracker.storage.sql-test
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [version-tracker.crypto :as crypto]
            [version-tracker.storage :as storage]
            [version-tracker.test-utils :as test-utils]))

(use-fixtures :once test-utils/schema-validation-fixture)

(deftest create-user
  (test-utils/with-sql-storage storage
    (let [username "foo"
          password-hash "bar"
          github-token "token"
          _ (is (nil? (storage/create-user! storage username password-hash github-token)))
          decrypter (test-utils/decrypter)
          users (jdbc/query storage
                            ["SELECT username, password_hash, encrypted_github_token FROM users WHERE username=?" "foo"])
          user (first users)]
      (is (= 1 (count users)))
      (is (= username (:username user)))
      (is (= password-hash (:password_hash user)))
      (is (= github-token (->> user :encrypted_github_token (crypto/decrypt decrypter)))))))

(deftest find-user
  (test-utils/with-sql-storage storage
    (testing "user exists"
      (let [username "foo"]
        (storage/create-user! storage username "bar" "token")
        (is (= username (:username (storage/find-user storage username))))
        (is (uuid? (:id (storage/find-user storage username))))))
    (testing "user does not exist"
      (is (nil? (storage/find-user storage "notfoo"))))))

(deftest user-exists?
  (test-utils/with-sql-storage storage
    (testing "user exists"
      (let [username "foo"]
        (storage/create-user! storage username "bar" "token")
        (is (true? (storage/user-exists? storage username)))))
    (testing "user does not exist"
      (is (false? (storage/user-exists? storage "notfoo"))))))

(deftest add-tracked-repo
  (test-utils/with-sql-storage storage
    (let [username "foo"
          _ (storage/create-user! storage username "bar" "token")
          github-id "someid"
          {user-id :id} (storage/find-user storage username)
          _ (is (some? user-id))]
      (testing "First call"
        (is (= [] (storage/find-tracked-repo-github-ids storage user-id)))
        (is (nil? (storage/add-tracked-repo storage user-id github-id)))
        (is (= [{:github-id github-id}]
               (storage/find-tracked-repo-github-ids storage user-id))))
      (testing "Second call"
        (is (nil? (storage/add-tracked-repo storage user-id github-id))
            "Should be idempotent")
        (is (= [{:github-id github-id}]
               (storage/find-tracked-repo-github-ids storage user-id)))))))
