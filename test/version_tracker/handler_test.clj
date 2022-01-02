(ns version-tracker.handler-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.jdbc :as jdbc]
            [ring.mock.request :as mock]
            [version-tracker.handler :as handler]
            [version-tracker.test-utils :as test-utils]))

(deftest test-app
  (testing "main route"
    (let [response (handler/app (mock/request :get "/"))]
      (is (= (:status response) 200))
      (is (= (:body response) "Hello World"))))

  (testing "not-found route"
    (let [response (handler/app (mock/request :get "/invalid"))]
      (is (= (:status response) 404)))))
