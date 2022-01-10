(ns version-tracker.e2e-test
  (:require [buddy.hashers :as hashers]
            [clj-http.client :as http]
            [clojure.data.json :as json]
            [clojure.java.jdbc :as jdbc]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [version-tracker.fakes.fake-github :as fake-github]
            [version-tracker.model.user :as user]
            [version-tracker.test-utils :as test-utils]
            [clojure.string :as str]
            [version-tracker.crypto :as crypto]))

(use-fixtures :once test-utils/schema-validation-fixture)

(defn- base-url [system]
  (let [port (get-in system [:config :webserver :port])]
    (format "http://localhost:%d" port)))

(defn- signup [system username password github-token]
  (let [url (str (base-url system) "/users")
        payload (json/write-str {:username username
                                 :password password
                                 :github_token github-token})]
    (http/post url {:body payload
                    :content-type :json})))

(deftest hello-test
  (test-utils/with-system system
    (testing "authenticated"
      (let [username "foo"
            password "bar"
            _ (user/create-user! (:storage system)
                                 "foo"
                                 "bar"
                                 "token")
            url (str (base-url system) "/")
            resp (http/get url {:basic-auth [username password]})]
        (is (= "Hello foo" (:body resp)))
        (is (= 200 (:status resp)))))
    (testing "unauthenticated"
      (let [url (str (base-url system) "/")
            resp (http/get url {:throw-exceptions false})]
        (is (= 401 (:status resp)))))))

(deftest signup-test
  (test-utils/with-system system
    (let [url (str (base-url system) "/users")
          username "foo"
          password "bar"
          token "token"
          decrypter (test-utils/decrypter)
          payload (json/write-str {:username username
                                   :password password
                                   :github_token token})]
      (testing "first time signup"
        (let [resp (http/post url {:body payload
                                   :content-type :json
                                   :throw-exceptions false})
              db-user (first (jdbc/query (:storage system)
                                         ["SELECT encrypted_github_token, password_hash FROM users WHERE username=?" username]))]
          (is (= 200 (:status resp)))
          (is (some? db-user))
          (is (true? (hashers/check password (:password_hash db-user))))
          (is (= token (crypto/decrypt decrypter (:encrypted_github_token db-user))))))
      (testing "duplicate signup"
        (let [resp (http/post url {:body payload
                                   :content-type :json
                                   :throw-exceptions false})]
          (is (= 400 (:status resp))))))))

(deftest track-repo-test
  (test-utils/with-system system
    (let [url (str (base-url system) "/repos")
          username "foo"
          password "bar"
          payload (json/write-str {:owner "microsoft"
                                   :repo_name "vscode"})]
      (signup system username password fake-github/good-token)
      (testing "first time tracking"
        (let [resp (http/post url {:body payload
                                   :content-type :json
                                   :throw-exceptions false
                                   :basic-auth [username password]})
              db-repo (first (jdbc/query (:storage system)
                                         [(str/join "\n"
                                                    ["SELECT tracked_repos.github_id"
                                                     "FROM tracked_repos"
                                                     "JOIN users ON tracked_repos.user_id = users.id"
                                                     "WHERE tracked_repos.github_id = ?"
                                                     "      AND users.username = ?"])
                                          fake-github/repo-github-id
                                          username]))]
          (is (= 200 (:status resp)))
          (is (some? db-repo)))))))

(deftest repo-summaries-test
  (test-utils/with-system system
    (let [url (str (base-url system) "/repos")
          username "foo"
          password "bar"]
      (signup system username password fake-github/good-token)
      
      (testing "no repos tracked"
        (let [resp (http/get url {:basic-auth [username password]
                                  :throw-exceptions false})]
          (is (= 200 (:status resp)))
          (is (= {:items []}
                 (-> resp
                     :body
                     (json/read-str :key-fn keyword))))))
      (testing "one repo tracked"
        ;; track the repo
        (is (= 200 (:status (http/post url {:body (json/write-str {:owner "microsoft"
                                                                   :repo_name "vscode"})
                                            :content-type :json
                                            :throw-exceptions false
                                            :basic-auth [username password]}))))
        
        (let [resp (http/get url {:basic-auth [username password]
                                  :throw-exceptions false})]
          (is (= 200 (:status resp)))
          (is (= {:items [{:owner "microsoft"
                           :repo_name "vscode"
                           :latest_release
                           {:version "1.63.2"
                            :date "2021-12-16T17:51:28Z"}}]}
                 (-> resp
                     :body
                     (json/read-str :key-fn keyword)))))))))

(deftest not-found-test
  (test-utils/with-system system
    (let [url (str (base-url system) "/invalid")
          resp (http/get url {:throw-exceptions false})]
      (is (= 404 (:status resp))))))
