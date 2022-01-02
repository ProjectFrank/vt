(ns version-tracker.storage.sql-test
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.test :refer [deftest is use-fixtures]]
            [version-tracker.storage :as storage]
            [version-tracker.test-utils :as test-utils]))

(use-fixtures :once test-utils/schema-validation-fixture)

(deftest create-user
  (test-utils/with-sql-storage storage
    (is (= 1 (storage/create-user storage "foo" "bar")))
    (let [users (jdbc/query storage
                            ["SELECT username, password_hash FROM users WHERE username=?" "foo"])]
      (is (= [{:username "foo" :password_hash "bar"}]
             users)))))
