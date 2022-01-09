(ns version-tracker.handler
  (:require [reitit.ring :as router]
            [ring.middleware.basic-authentication :as ring-basic]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :as json]
            [schema.core :as s]
            [version-tracker.github :as github]
            [version-tracker.handler.signup :as signup]
            [version-tracker.handler.track-repo :as track-repo]
            [version-tracker.model.user :as user]
            [version-tracker.storage :as storage])
  (:import [version_tracker.github Client]))

(defn handle-hello [request]
  (let [{:keys [::user/username]} (:basic-authentication request)]
    {:status 200 :body (format "Hello %s" username)
     :headers {"Content-Type" "text/html"}}))

(defn wrap-basic-authentication
  "Adds basic authentication. Requires storage/wrap-storage as an outer
  layer the middleware stack."
  [handler]
  ;; adapter necessary to make wrap-basic-authentication work with
  ;; injecting dependencies in the request.
  (fn [request]
    (let [storage (storage/storage request)
          wrapped-handler (ring-basic/wrap-basic-authentication handler
                                                                (partial user/authenticate-user storage))]
      (wrapped-handler request))))

(s/defn router [github-client :- Client]
  (router/router
   [["/" {:get (wrap-basic-authentication handle-hello)}]
    ["/users" {:post signup/handler}]
    ["/repos" {:post (-> track-repo/handler
                         (github/wrap-client github-client)
                         wrap-basic-authentication)}]]))

(def default-handler (router/create-default-handler
                      {:not-found (constantly {:status 404 :body "Not Found"})}))

(s/defn handler [github-client :- Client]
  (router/ring-handler (router github-client) default-handler))

(defn wrap-print-exceptions
  "Prints exceptions thrown by handler. Useful for debugging tests."
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (println e)
        (throw e)))))

(s/defn app [github-client :- Client]
  (-> (handler github-client)
      wrap-print-exceptions
      json/wrap-json-response
      (json/wrap-json-params {:key-fn keyword})
      (wrap-defaults api-defaults)))
