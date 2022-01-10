(ns version-tracker.model.user
  (:require [buddy.hashers :as hashers]
            [clojure.set :refer [rename-keys]]
            [schema.core :as s]
            [version-tracker.crypto :as crypto]
            [version-tracker.release-client :as release-client]
            [version-tracker.storage :as storage]))

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

(s/defn track-repo :- (s/enum ::tracked ::repo-not-found)
  [storage :- (s/protocol storage/Storage)
   release-client :- (s/protocol release-client/ReleaseClient)
   user-id :- Id
   owner :- s/Str
   repo-name :- s/Str]
  (if-let [repo-id (release-client/get-repo-id release-client owner repo-name)]
    (do
      (storage/add-tracked-repo storage user-id repo-id)
      ::tracked)
    ::repo-not-found))

(s/defn list-tracked-repos :- [{::owner s/Str
                                ::name s/Str
                                ::latest-release
                                {::version s/Str
                                 ::date s/Str}}]
  [storage :- (s/protocol storage/Storage)
   release-client :- (s/protocol release-client/ReleaseClient)
   user-id :- Id]
  (let [github-ids (->> (storage/find-tracked-repo-github-ids storage user-id)
                        (map :github-id))
        repo-summaries (release-client/get-repo-summaries release-client github-ids)]
    (map (fn [client-summary]
           (-> client-summary
               (rename-keys {:owner ::owner
                             :name ::name
                             :latest-release ::latest-release})
               (update ::latest-release rename-keys {:version ::version
                                                     :date ::date})))
         repo-summaries)))
