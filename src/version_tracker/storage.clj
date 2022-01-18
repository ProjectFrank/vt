(ns version-tracker.storage
  (:require [schema.core :as s]
            [version-tracker.crypto :as crypto])
  (:import [java.time Instant]))

(defn wrap-storage [handler storage]
  (fn [request]
    (handler (assoc request ::storage storage))))

(defn storage [request]
  (::storage request))

(defprotocol Storage
  (-create-user [_this username password-hash github-token])
  (-user-exists? [_this username])
  (-find-user [_this username])
  (-add-tracked-repo [_this user-id github-id])
  (-find-tracked-repos [_this user-id]))

(s/def Store (s/protocol Storage))

(s/defn create-user! :- (s/eq nil)
  [store :- Store
   username :- s/Str
   password-hash :- s/Str
   github-token :- s/Str]
  (-create-user store
                username
                password-hash
                github-token))

(s/defn user-exists? :- s/Bool
  [store :- Store
   username :- s/Str]
  (-user-exists? store username))

(s/defn find-user :- (s/maybe {:id s/Uuid
                               :username s/Str
                               :password-hash s/Str
                               :encrypted-github-token crypto/Bytes})
  [store :- Store
   username :- s/Str]
  (-find-user store username))

(s/defn add-tracked-repo :- {:id s/Uuid}
  [store :- Store
   user-id :- s/Uuid
   github-id :- s/Str]
  (-add-tracked-repo store user-id github-id))

(s/defn find-tracked-repos :- [{:github-id s/Str
                                :id s/Uuid
                                :last-seen (s/maybe Instant)}]
  [store :- Store
   user-id :- s/Uuid]
  (-find-tracked-repos store user-id))
