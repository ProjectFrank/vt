(ns version-tracker.model.user-test
  (:require [buddy.hashers :as hashers]
            [clojure.java.jdbc :as jdbc]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [version-tracker.crypto :as crypto]
            [version-tracker.model.user :as user]
            [version-tracker.release-client :as release-client]
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

(def stub-release-client
  (reify release-client/ReleaseClient
    (-get-repo-id [_this owner name]
      (if (and (= "microsoft" owner)
               (= "vscode" name))
        "MDEwOlJlcG9zaXRvcnk0MTg4MTkwMA=="
        nil))
    (-get-repo-summaries [_this github-ids]
      (if (= ["MDEwOlJlcG9zaXRvcnk0MTg4MTkwMA=="] github-ids)
        [{:owner "microsoft"
          :name "vscode"
          :latest-release
          {:version "1.63.2"
           :date "2021-12-16T17:51:28Z"}}]
        []))))

(deftest track-repo-test
  (test-utils/with-sql-storage storage
    (let [username "foo"
          password "bar"
          github-token "token"
          _ (user/create-user! storage username password github-token)
          user (user/authenticate-user storage username password)]
      (testing "repo not found"
        (is (= [] (user/list-tracked-repos storage
                                           stub-release-client
                                           (::user/id user))))
        (is (= {::user/result ::user/repo-not-found}
               (user/track-repo storage
                                stub-release-client
                                (::user/id user)
                                "notfound"
                                "notfound")))
        (is (= [] (user/list-tracked-repos storage
                                           stub-release-client
                                           (::user/id user)))))
      (testing "repo found"
        (is (= [] (user/list-tracked-repos storage
                                           stub-release-client
                                           (::user/id user))))
        (let [result (user/track-repo storage
                                  stub-release-client
                                  (::user/id user)
                                  "microsoft"
                                  "vscode")]
          (is (= ::user/tracked (::user/result result)))
          (is (uuid? (get-in result [::user/repo ::user/id]))))
        (is (= [{::user/owner "microsoft"
                 ::user/name "vscode"
                 ::user/latest-release
                 {::user/version "1.63.2"
                  ::user/date "2021-12-16T17:51:28Z"}}]
               (user/list-tracked-repos storage
                                        stub-release-client
                                        (::user/id user))))))))
