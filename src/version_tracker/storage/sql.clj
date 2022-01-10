(ns version-tracker.storage.sql
  (:require [clojure.java.io :as io]
            [clojure.set :refer [rename-keys]]
            [com.stuartsierra.component :as component]
            [hikari-cp.core :as cp]
            [hugsql.core :as hugsql]
            [schema.core :as s]
            [version-tracker.config :as config]
            [version-tracker.crypto :as crypto]
            [version-tracker.storage :as storage]))

(declare create-user*)
(declare count-users*)
(declare find-user*)
(declare count-tracked-repo-by-github-id*)

(hugsql/def-db-fns (io/resource "queries.sql"))

(s/defrecord PostgresStorage [datasource encrypter config :- config/Postgres]
  storage/Storage
  (-user-exists? [this username]
    (not= 0 (:count (count-users* this {:username username}))))
  (-create-user [this username password-hash github-token]
    (create-user* this
                  {:username username
                   :password-hash password-hash
                   :encrypted-github-token (crypto/encrypt encrypter github-token)})
    nil)
  (-find-user [this username]
    (if-let [result (find-user* this {:username username})]
      (rename-keys result {:password_hash :password-hash
                           :encrypted_github_token :encrypted-github-token})
      nil))
  (-add-tracked-repo [this user-id github-id]
    (when (= 0 (:count (count-tracked-repo-by-github-id* this {:user-id user-id
                                                               :github-id github-id})))
     (add-tracked-repo* this {:user-id user-id, :github-id github-id}))
    nil)
  (-find-tracked-repo-github-ids [this user-id]
    (->> (find-tracked-repo-github-ids* this {:user-id user-id})
         (mapv #(rename-keys % {:github_id :github-id}))))

  component/Lifecycle
  (start [this]
    (let [datasource (cp/make-datasource {:server-name (:host config)
                                          :port-number (:port config)
                                          :database-name (:database config)
                                          :username (:user config)
                                          :password (:password config)
                                          :adapter "postgresql"})]
      (assoc this :datasource datasource)))
  (stop [this]
    (cp/close-datasource datasource)
    (dissoc this :datasource)))

(s/defn postgres-storage [config :- config/Postgres]
  (map->PostgresStorage {:config config}))
