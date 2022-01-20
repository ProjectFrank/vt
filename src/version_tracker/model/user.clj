(ns version-tracker.model.user
  (:require [buddy.hashers :as hashers]
            [clojure.set :refer [rename-keys]]
            [schema.core :as s]
            [version-tracker.crypto :as crypto]
            [version-tracker.release-client :as release-client]
            [version-tracker.storage :as storage]
            [version-tracker.util.time :as time-util])
  (:import [java.time Instant]))

(s/def Id s/Uuid)

(s/def User
  {::id Id
   ::username s/Str
   ::encrypted-github-token crypto/Bytes})

(s/defn ^:private password-hash :- s/Str
  [password :- s/Str]
  (hashers/derive password {:alg :argon2id}))

(s/defn create-user! :- (s/enum ::created ::user-exists)
  [storage :- (s/protocol storage/Storage)
   username :- s/Str
   password :- s/Str
   github-token :- s/Str]
  (if (storage/user-exists? storage username)
    ::user-exists
    (let [pw-hash (password-hash password)]
      (storage/create-user! storage username pw-hash github-token)
      ::created)))

(s/defn authenticate-user :- (s/maybe User)
  [storage :- (s/protocol storage/Storage)
   username :- s/Str
   password-attempt :- s/Str]
  (let [{:keys [id password-hash encrypted-github-token]} (storage/find-user storage username)
        password-correct? (hashers/check password-attempt password-hash)]
    (if password-correct?
      {::id id
       ::username username
       ::encrypted-github-token encrypted-github-token}
      nil)))

(s/def TrackRepoResult
  (s/conditional
   #(= ::tracked (::result %))
   {::repo {::id s/Uuid}
    ::result (s/eq ::tracked)}

   :else
   {::result (s/eq ::repo-not-found)}))

(s/defn track-repo :- TrackRepoResult
  [storage :- (s/protocol storage/Storage)
   release-client :- (s/protocol release-client/ReleaseClient)
   user-id :- Id
   owner :- s/Str
   repo-name :- s/Str]
  (if-let [repo-id (release-client/get-repo-id release-client owner repo-name)]
    (let [{:keys [id]} (storage/add-tracked-repo storage user-id repo-id)]
      {::result ::tracked
       ::repo {::id id}})
    {::result ::repo-not-found}))

(s/defn list-tracked-repos :- [{::id s/Uuid
                                ::last-seen (s/maybe Instant)
                                ::owner s/Str
                                ::name s/Str
                                ::latest-release
                                {::version s/Str
                                 ::date Instant}}]
  [storage :- (s/protocol storage/Storage)
   release-client :- (s/protocol release-client/ReleaseClient)
   user-id :- Id]
  (let [by-github-id (->> (storage/find-tracked-repos storage user-id)
                          (group-by :github-id))
        repo-summaries (release-client/get-repo-summaries release-client (keys by-github-id))
        summaries-by-external-id (group-by :external-id repo-summaries)]
    (map (fn [external-id]
           (let [rc-summary (first (get summaries-by-external-id external-id))
                 storage-summary (first (get by-github-id external-id))]
             (-> rc-summary
                 (rename-keys {:owner ::owner
                               :name ::name
                               :latest-release ::latest-release})
                 (update ::latest-release rename-keys {:version ::version
                                                       :date ::date})
                 (assoc ::id (:id storage-summary)
                        ::last-seen (:last-seen storage-summary))
                 (dissoc :external-id))))
         (keys summaries-by-external-id))))

(s/defn mark-repo-seen :- (s/eq nil)
  [storage :- (s/protocol storage/Storage)
   user-id :- Id
   repo-id :- Id]
  (storage/set-tracked-repo-seen storage
                                 user-id
                                 repo-id
                                 (time-util/now)))
