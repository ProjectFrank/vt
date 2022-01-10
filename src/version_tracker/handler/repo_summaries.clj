(ns version-tracker.handler.repo-summaries
  (:require [clojure.set :refer [rename-keys]]
            [version-tracker.github :as github]
            [version-tracker.model.user :as user]
            [version-tracker.storage :as storage]))

(defn response-body [repo-summary]
  (-> repo-summary
      (rename-keys {::user/owner :owner
                    ::user/name :repo_name
                    ::user/latest-release :latest_release})
      (update :latest_release rename-keys {::user/version :version
                                           ::user/date :date})))

(defn handler [request]
  (let [storage (storage/storage request)
        github-client (::github/client request)
        user-id (-> request :basic-authentication ::user/id)
        result (user/list-tracked-repos storage github-client user-id)]
    {:status 200, :body {:items (map response-body result)}}))
