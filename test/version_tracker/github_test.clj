(ns version-tracker.github-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [version-tracker.fakes.fake-github :as fake-github]
            [version-tracker.github :as github]
            [version-tracker.test-utils :as test-utils]
            [version-tracker.release-client :as release-client]
            [version-tracker.config :as config]))

(use-fixtures :once test-utils/schema-validation-fixture)

(deftest get-repo-id-test
  (let [token (-> (config/load-config {:profile :test})
                  (get-in [:github :token]))
        {:keys [server base-url]} (fake-github/start {:port 3001, :token token})]
    (try
      (testing "Repo exists"
        (let [release-client (github/github-client {:base-url base-url
                                                    :token token})]
          (is (= "MDEwOlJlcG9zaXRvcnk0MTg4MTkwMA=="
                 (release-client/get-repo-id release-client "microsoft" "vscode")))))
      (finally
        (fake-github/stop server)))))
