(ns version-tracker.storage.sql-test
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [version-tracker.storage :as storage]
            [version-tracker.test-utils :as test-utils]))

(use-fixtures :once test-utils/schema-validation-fixture)

(deftest create-user
  (test-utils/with-sql-storage storage
    (is (nil? (storage/create-user! storage "foo" "bar")))
    (let [users (jdbc/query storage
                            ["SELECT username, password_hash FROM users WHERE username=?" "foo"])]
      (is (= [{:username "foo" :password_hash "bar"}]
             users)))))

(deftest find-user
  (test-utils/with-sql-storage storage
    (testing "user exists"
      (let [username "foo"]
        (storage/create-user! storage username "bar")
        (is (= username (:username (storage/find-user storage username))))
        (is (uuid? (:id (storage/find-user storage username))))))
    (testing "user does not exist"
      (is (nil? (storage/find-user storage "notfoo"))))))

(deftest user-exists?
  (test-utils/with-sql-storage storage
    (testing "user exists"
      (let [username "foo"]
        (storage/create-user! storage username "bar")
        (is (true? (storage/user-exists? storage username)))))
    (testing "user does not exist"
      (is (false? (storage/user-exists? storage "notfoo"))))))

(deftest add-tracked-repo
  (test-utils/with-sql-storage storage
    (let [username "foo"
          _ (storage/create-user! storage username "bar")
          github-id "someid"
          {user-id :id} (storage/find-user storage "foo")
          _ (is (some? user-id))
          tracked-repos #(jdbc/query storage ["SELECT user_id, github_id FROM tracked_repos WHERE user_id=? AND github_id=?" user-id github-id])]
      (testing "First call"
        (is (= [] (tracked-repos)))
        (is (nil? (storage/add-tracked-repo storage user-id github-id)))
        (is (= [{:user_id user-id
                 :github_id github-id}]
               (tracked-repos))))
      (testing "Second call"
        (is (nil? (storage/add-tracked-repo storage user-id github-id))
            "Should be idempotent")
        (is (= [{:user_id user-id
                 :github_id github-id}]
               (tracked-repos)))))))
