(ns version-tracker.handler.signup
  (:require [clojure.data.json :as json]
            [schema.core :as s]
            [version-tracker.model.user :as user]
            [version-tracker.storage :as storage]))

(s/def SignupRequest
  {:username (s/constrained s/Str #(< (count %) 72))
   :password (s/constrained s/Str #(< (count %) 72))})

(defn handle-signup [request]
  (let [storage (storage/storage request)
        {:keys [username password] :as payload} (json/read-str (slurp (:body request)) :key-fn keyword)
        valid? (not (s/check SignupRequest payload))]
    (if-not valid?
      {:status 400}
      (let [result (user/create-user! storage username password)]
        (if (= ::user/created result)
          {:status 200}
          {:status 400 :body "User exists"})))))
