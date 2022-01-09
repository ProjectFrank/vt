(ns version-tracker.github-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [version-tracker.fakes.fake-github :as fake-github]
            [version-tracker.github :as github]
            [version-tracker.release-client :as release-client]
            [version-tracker.test-utils :as test-utils]))

(use-fixtures :once test-utils/schema-validation-fixture)

(deftest get-repo-id-test
  (let [{:keys [base-url server]} (fake-github/start)]
    (try
      (testing "Repo exists"
        (let [github-client (github/map->Client {:config {:base-url base-url}
                                                 :token fake-github/good-token})]
          (is (= "MDEwOlJlcG9zaXRvcnk0MTg4MTkwMA=="
                 (release-client/get-repo-id github-client "microsoft" "vscode")))))
      (finally
        (fake-github/stop server)))))
