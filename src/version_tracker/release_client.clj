(ns version-tracker.release-client
  (:require [schema.core :as s]))

(defprotocol ReleaseClient
  (-get-repo-id [_this owner name])
  (-get-repo-summaries [_this repo-ids]))

(s/defn get-repo-id :- (s/maybe s/Str)
  [release-client :- (s/protocol ReleaseClient)
   owner :- s/Str
   repo-name :- s/Str]
  (-get-repo-id release-client owner repo-name))

(s/defn get-repo-summaries :- [{:owner s/Str
                                :name s/Str
                                :latest-release
                                {:version s/Str
                                 :date s/Str}}]
  [release-client :- (s/protocol ReleaseClient)
   repo-ids :- [s/Str]]
  (-get-repo-summaries release-client repo-ids))

(s/defn wrap-release-client
  [handler
   release-client :- (s/protocol ReleaseClient)]
  (fn [request]
    (handler (assoc request ::release-client release-client))))

(s/defn release-client :- (s/protocol ReleaseClient)
  [request]
  (::release-client request))
