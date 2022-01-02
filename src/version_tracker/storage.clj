(ns version-tracker.storage)

(defn wrap-storage [handler storage]
  (fn [request]
    (handler (assoc request ::storage storage))))

(defn storage [request]
  (::storage request))

(defprotocol Storage
  (create-user [_this username password-hash])
  (user-exists? [_this username]))
