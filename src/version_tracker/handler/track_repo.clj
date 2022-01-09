(ns version-tracker.handler.track-repo
  (:require [schema.core :as s]
            [version-tracker.github :as github]
            [version-tracker.model.user :as user]
            [version-tracker.release-client :as release-client]
            [version-tracker.storage :as storage]))

(s/def TrackRepoRequest
  {:owner s/Str
   :repo_name s/Str})

(defn handler [{:keys [json-params] :as request}]
  (let [{:keys [owner repo_name]} json-params
        storage (storage/storage request)
        github-client (github/client-from-request request)
        {user-id ::user/id} (:basic-authentication request)
        valid? (not (s/check TrackRepoRequest json-params))]
    (if-not valid?
      {:status 400}
      (do
        (user/track-repo storage
                         github-client
                         user-id
                         owner
                         repo_name)
        {:status 200}))))
