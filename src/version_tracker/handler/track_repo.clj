(ns version-tracker.handler.track-repo
  (:require [schema.core :as s]
            [version-tracker.github :as github]
            [version-tracker.model.user :as user]
            [version-tracker.storage :as storage]))

(s/def TrackRepoRequest
  {:owner s/Str
   :repo_name s/Str})

(s/def TrackRepoResponse {:id s/Str})

(s/defn handler :- (s/conditional
                    #(= 200 (:status %))
                    {:body TrackRepoResponse
                     s/Any s/Any}

                    :else
                    {:body {:message s/Str}
                     s/Any s/Any})
  [{:keys [json-params] :as request} :- {:json-params TrackRepoRequest
                                         s/Any s/Any}]
  (let [{:keys [owner repo_name]} json-params
        storage (storage/storage request)
        github-client (github/client-from-request request)
        {user-id ::user/id} (:basic-authentication request)
        valid? (not (s/check TrackRepoRequest json-params))]
    (if-not valid?
      {:status 400, :body {:message "Invalid request."}}
      (let [result (user/track-repo storage
                                    github-client
                                    user-id
                                    owner
                                    repo_name)]
        (if (= ::user/repo-not-found (::user/result result))
          {:status 400, :body {:message "Repository not found."}}
          {:status 200
           :body
           {:id (-> result
                    (get-in [::user/repo ::user/id])
                    str)}})))))
