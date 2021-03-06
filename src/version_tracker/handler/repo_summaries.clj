(ns version-tracker.handler.repo-summaries
  (:require [clojure.set :refer [rename-keys]]
            [schema.core :as s]
            [version-tracker.github :as github]
            [version-tracker.model.user :as user]
            [version-tracker.storage :as storage])
  (:import [java.time Instant]))

(s/def RepoSummariesResponse
  {:items
   [{:id s/Uuid
     :owner s/Str
     :repo_name s/Str
     :last_seen (s/maybe Instant)
     :latest_release
     {:version s/Str
      :date Instant}}]})

(defn response-body
  [repo-summary]
  (-> repo-summary
      (rename-keys {::user/owner :owner
                    ::user/name :repo_name
                    ::user/id :id
                    ::user/last-seen :last_seen
                    ::user/latest-release :latest_release})
      (update :latest_release rename-keys {::user/version :version
                                           ::user/date :date})))

(s/defn handler :- {:body RepoSummariesResponse
                    s/Any s/Any}
  [request]
  (let [storage (storage/storage request)
        github-client (github/client-from-request request)
        user-id (-> request :basic-authentication ::user/id)
        result (user/list-tracked-repos storage github-client user-id)]
    {:status 200, :body {:items (map response-body result)}}))
