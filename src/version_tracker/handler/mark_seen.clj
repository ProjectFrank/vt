(ns version-tracker.handler.mark-seen
  (:require [reitit.core :as reitit]
            [version-tracker.model.user :as user]
            [version-tracker.storage :as storage])
  (:import [java.util UUID]))

(defn handler [request]
  (let [user-id (get-in request [:basic-authentication ::user/id])
        repo-id (try
                  (-> request
                      (get-in [::reitit/match :path-params :repo-id])
                      UUID/fromString)
                  (catch Exception _
                    nil))
        storage (storage/storage request)]
    (if-not repo-id
      {:status 400, :body {:message "Repo ID must be a valid UUID."}}
      (do
        (user/mark-repo-seen storage user-id repo-id)
        {:status 200}))))
