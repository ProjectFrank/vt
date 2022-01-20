(ns version-tracker.util.time
  (:require [java-time :as time]
            [schema.core :as s])
  (:import [java.sql Timestamp]
           [java.time Instant OffsetDateTime ZoneOffset]))

(s/defn now :- Instant
  "Useful seam for stubbing out now in tests."
  []
  (time/instant))

(s/defn to-sql :- (s/maybe OffsetDateTime)
  [inst :- (s/maybe Instant)]
  (if inst
    (OffsetDateTime/ofInstant inst ZoneOffset/UTC)
    nil))

(s/defn from-sql :- (s/maybe Instant)
  [timestamp :- (s/maybe Timestamp)]
  (if timestamp
    (time/instant timestamp)
    nil))
