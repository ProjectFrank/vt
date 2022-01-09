(ns version-tracker.model.user-test
  (:require [buddy.hashers :as hashers]
            [clojure.java.jdbc :as jdbc]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [version-tracker.crypto :as crypto]
            [version-tracker.model.user :as user]
            [version-tracker.test-utils :as test-utils]))

(use-fixtures :once test-utils/schema-validation-fixture)

(deftest create-user-test
  (let [username "foo"
        password "bar"
        github-token "token"]
    (test-utils/with-sql-storage storage
      (testing "first creation"
        (is (= ::user/created (user/create-user! storage
                                                 username
                                                 password
                                                 github-token)))
        (let [users (jdbc/query storage
                                ["SELECT username, password_hash FROM users WHERE username=?" username])
              password-hash (-> users first :password_hash)]
          (is (= ["foo"] (map :username users)))
          (is (true? (hashers/check password password-hash)))))
      (testing "duplicate creation"
        (is (= ::user/user-exists (user/create-user! storage
                                                     username
                                                     password
                                                     github-token)))))))

(deftest authenticate-user-test
  (let [username "foo"
        password "bar"
        github-token "token"]
    (test-utils/with-sql-storage storage
      (user/create-user! storage username password github-token)
      (let [{:keys [id]} (first (jdbc/query storage
                                            ["SELECT id FROM users WHERE username=?" username]))
            result (user/authenticate-user storage username password)
            decrypter (test-utils/decrypter)]
        (testing "correct password"
          (is (some? id))
          (is (= id (::user/id result)))
          (is (= username (::user/username result)))
          (is (= github-token
                 (->> result
                      ::user/encrypted-github-token
                      (crypto/decrypt decrypter)))))
        (testing "incorrect password"
          (is (nil? (user/authenticate-user storage username "notbar"))))))))
