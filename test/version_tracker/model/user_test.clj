(ns version-tracker.model.user-test
  (:require [buddy.hashers :as hashers]
            [clojure.java.jdbc :as jdbc]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [java-time :as time]
            [version-tracker.crypto :as crypto]
            [version-tracker.model.user :as user]
            [version-tracker.release-client :as release-client]
            [version-tracker.test-utils :as test-utils]
            [version-tracker.storage :as storage]))

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

(deftest track-repo-test
  (test-utils/with-sql-storage storage
    (let [username "foo"
          password "bar"
          github-token "token"
          rc (reify release-client/ReleaseClient
               (-get-repo-id [_this owner name]
                 (if (and (= "microsoft" owner)
                          (= "vscode" name))
                   "MDEwOlJlcG9zaXRvcnk0MTg4MTkwMA=="
                   nil))
               (-get-repo-summaries [_this github-ids]
                 (if (= ["MDEwOlJlcG9zaXRvcnk0MTg4MTkwMA=="] github-ids)
                   [{:external-id "MDEwOlJlcG9zaXRvcnk0MTg4MTkwMA=="
                     :owner "microsoft"
                     :name "vscode"
                     :latest-release
                     {:version "1.63.2"
                      :date (time/instant "2021-12-16T17:51:28Z")}}]
                   [])))
          _ (user/create-user! storage username password github-token)
          user (user/authenticate-user storage username password)]
      (testing "repo not found"
        (is (= [] (user/list-tracked-repos storage
                                           rc
                                           (::user/id user))))
        (is (= {::user/result ::user/repo-not-found}
               (user/track-repo storage
                                rc
                                (::user/id user)
                                "notfound"
                                "notfound")))
        (is (= [] (user/list-tracked-repos storage
                                           rc
                                           (::user/id user)))))
      (testing "repo found"
        (is (= [] (user/list-tracked-repos storage
                                           rc
                                           (::user/id user))))
        (let [result (user/track-repo storage
                                      rc
                                      (::user/id user)
                                      "microsoft"
                                      "vscode")]
          (is (= ::user/tracked (::user/result result)))
          (is (uuid? (get-in result [::user/repo ::user/id])))
          (is (= [{::user/id (get-in result [::user/repo ::user/id])
                   ::user/owner "microsoft"
                   ::user/name "vscode"
                   ::user/last-seen nil
                   ::user/latest-release
                   {::user/version "1.63.2"
                    ::user/date (time/instant "2021-12-16T17:51:28Z")}}]
                 (user/list-tracked-repos storage
                                          rc
                                          (::user/id user)))))))))

(deftest list-tracked-repos-test
  (let [stub-storage
        (reify
          storage/Storage
          (-find-tracked-repos [_ user-id]
            (if-not (= user-id #uuid "38e38f1e-4d96-4e82-95cf-89931b63f652")
              nil
              [{:github-id "github-id-1"
                :id #uuid "f55a4a11-4095-4a0f-b792-c82224f19809"
                :last-seen (time/instant "2021-01-01T00:00:00.000-00:00")}
               {:github-id "github-id-2"
                :id #uuid "9380ca11-714a-44f3-ad8f-e95ba14eb18b"
                :last-seen (time/instant "2022-01-01T00:00:00.000-00:00")}])))
        stub-release-client
        (reify release-client/ReleaseClient
          (-get-repo-summaries [_this repo-ids]
            (if-not (= (set repo-ids) #{"github-id-1" "github-id-2"})
              nil
              [{:external-id "github-id-1"
                :owner "microsoft"
                :name "vscode"
                :latest-release
                {:version "1.63.2"
                 :date (time/instant "2021-12-16T17:51:28Z")}}
               {:external-id "github-id-2"
                :owner "yogthos"
                :name "selmer"
                :latest-release
                {:version "0.1.1"
                 :date (time/instant "2021-12-17T17:51:28Z")}}])))]
   (is (= [{::user/id #uuid "f55a4a11-4095-4a0f-b792-c82224f19809"
            ::user/last-seen (time/instant "2021-01-01T00:00:00.000-00:00")
            ::user/owner "microsoft"
            ::user/name "vscode"
            ::user/latest-release
            {::user/version "1.63.2"
             ::user/date (time/instant "2021-12-16T17:51:28Z")}}
           {::user/id #uuid "9380ca11-714a-44f3-ad8f-e95ba14eb18b"
            ::user/last-seen (time/instant "2022-01-01T00:00:00.000-00:00")
            ::user/owner "yogthos"
            ::user/name "selmer"
            ::user/latest-release
            {::user/version "0.1.1"
             ::user/date (time/instant "2021-12-17T17:51:28Z")}}]
          (user/list-tracked-repos stub-storage
                                   stub-release-client
                                   #uuid "38e38f1e-4d96-4e82-95cf-89931b63f652")))))
