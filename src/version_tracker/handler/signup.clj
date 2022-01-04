(ns version-tracker.handler.signup
  (:require [schema.core :as s]
            [version-tracker.model.user :as user]
            [version-tracker.storage :as storage]))

(s/def SignupRequest
  {:username (s/constrained s/Str #(< (count %) 72))
   :password (s/constrained s/Str #(< (count %) 72))})

(defn handler [{:keys [json-params] :as request}]
  (let [storage (storage/storage request)
        {:keys [username password]} json-params
        valid? (not (s/check SignupRequest json-params))]
    (if-not valid?
      {:status 400}
      (let [result (user/create-user! storage username password)]
        (if (= ::user/created result)
          {:status 200}
          {:status 400 :body "User exists"})))))
