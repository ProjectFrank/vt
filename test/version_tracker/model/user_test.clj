(ns version-tracker.model.user-test
  (:require [buddy.hashers :as hashers]
            [clojure.java.jdbc :as jdbc]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [version-tracker.model.user :as user]
            [version-tracker.test-utils :as test-utils]))

(use-fixtures :once test-utils/schema-validation-fixture)

(deftest create-user-test
  (let [username "foo"
        password "bar"]
    (test-utils/with-sql-storage storage
      (testing "first creation"
        (is (= ::user/created (user/create-user! storage username password)))
        (let [users (jdbc/query storage
                                ["SELECT username, password_hash FROM users WHERE username=?" "foo"])
              password-hash (-> users first :password_hash)]
          (is (= ["foo"] (map :username users)))
          (is (true? (hashers/check password password-hash)))))
      (testing "duplicate creation"
        (is (= ::user/user-exists (user/create-user! storage username password)))))))
