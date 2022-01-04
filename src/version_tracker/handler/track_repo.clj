(ns version-tracker.handler.track-repo
  (:require [schema.core :as s]
            [version-tracker.model.user :as user]
            [version-tracker.release-client :as release-client]
            [version-tracker.storage :as storage]))

(s/def TrackRepoRequest
  {:owner s/Str
   :repo_name s/Str})

(defn handler [{:keys [json-params] :as request}]
  (let [{:keys [owner repo_name]} json-params
        storage (storage/storage request)
        release-client (release-client/release-client request)
        {user-id ::user/id} (:basic-authentication request)
        valid? (not (s/check TrackRepoRequest json-params))]
    (if-not valid?
      {:status 400}
      (do
        (user/track-repo storage
                         release-client
                         user-id
                         owner
                         repo_name)
        {:status 200}))))
