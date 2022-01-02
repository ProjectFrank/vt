(ns version-tracker.e2e-test
  (:require [buddy.hashers :as hashers]
            [clj-http.client :as http]
            [clojure.data.json :as json]
            [clojure.java.jdbc :as jdbc]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [version-tracker.test-utils :as test-utils]))

(use-fixtures :once test-utils/schema-validation-fixture)

(defn- base-url [system]
  (let [port (get-in system [:config :webserver :port])]
    (format "http://localhost:%d" port)))

(deftest hello-test
  (test-utils/with-system system
    (let [url (str (base-url system) "/")
          resp (http/get url)]
      (is (= "Hello World" (:body resp)))
      (is (= 200 (:status resp))))))

(deftest signup-test
  (test-utils/with-system system
    (let [url (str (base-url system) "/users")
          username "foo"
          password "bar"
          payload (json/write-str {:username username
                                   :password password})]
     (testing "first time signup"
       (let [resp (http/post url {:body payload
                                  :content-type :json
                                  :throw-exceptions false})
             db-user (first (jdbc/query (:storage system)
                                        ["SELECT password_hash FROM users WHERE username=?" username]))]
         (is (= 200 (:status resp)))
         (is (some? db-user))
         (is (true? (hashers/check password (:password_hash db-user))))))
     (testing "duplicate signup"
       (let [resp (http/post url {:body payload
                                  :content-type :json
                                  :throw-exceptions false})]
         (is (= 400 (:status resp))))))))
