(ns version-tracker.github-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [java-time :as time]
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

(deftest get-repo-summaries-test
  (let [{:keys [base-url server]} (fake-github/start)]
    (try
      (testing "Happy path"
        (let [github-client (github/map->Client {:config {:base-url base-url}
                                                 :token fake-github/good-token})]
          (is (= [{:external-id "MDEwOlJlcG9zaXRvcnk0MTg4MTkwMA=="
                   :owner "microsoft"
                   :name "vscode"
                   :latest-release
                   {:version "1.63.2"
                    :date (time/instant "2021-12-16T17:51:28Z")}}]
                 (release-client/get-repo-summaries github-client
                                                    ["MDEwOlJlcG9zaXRvcnk0MTg4MTkwMA=="])))))
      (finally
        (fake-github/stop server)))))
