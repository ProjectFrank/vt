(ns version-tracker.storage
  (:require [schema.core :as s]))

(defn wrap-storage [handler storage]
  (fn [request]
    (handler (assoc request ::storage storage))))

(defn storage [request]
  (::storage request))

(defprotocol Storage
  (-create-user [_this username password-hash])
  (-user-exists? [_this username])
  (-find-user [_this username])
  (-add-tracked-repo [_this user-id github-id]))

(s/def Store (s/protocol Storage))

(s/defn create-user! :- (s/eq nil)
  [store :- Store
   username :- s/Str
   password-hash :- s/Str]
  (-create-user store username password-hash))

(s/defn user-exists? :- s/Bool
  [store :- Store
   username :- s/Str]
  (-user-exists? store username))

(s/defn find-user :- (s/maybe {:id s/Uuid
                               :username s/Str
                               :password-hash s/Str})
  [store :- Store
   username :- s/Str]
  (-find-user store username))

(s/defn add-tracked-repo :- (s/eq nil)
  [store :- Store
   user-id :- s/Uuid
   github-id :- s/Str]
  (-add-tracked-repo store user-id github-id))
