(ns version-tracker.release-client
  (:require [schema.core :as s])
  (:import [java.time Instant]))

(defprotocol ReleaseClient
  (-get-repo-id [_this owner name])
  (-get-repo-summaries [_this repo-ids]))

(s/defn get-repo-id :- (s/maybe s/Str)
  [release-client :- (s/protocol ReleaseClient)
   owner :- s/Str
   repo-name :- s/Str]
  (-get-repo-id release-client owner repo-name))

(s/defn get-repo-summaries :- [{:external-id s/Str
                                :owner s/Str
                                :name s/Str
                                :latest-release
                                {:version s/Str
                                 :date Instant}}]
  [release-client :- (s/protocol ReleaseClient)
   repo-ids :- [s/Str]]
  (-get-repo-summaries release-client repo-ids))
