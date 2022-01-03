(ns version-tracker.handler
  (:require [reitit.middleware :as middleware]
            [reitit.ring :as router]
            [ring.middleware.basic-authentication :as ring-basic]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [schema.core :as s]
            [version-tracker.handler.signup :as signup]
            [version-tracker.model.user :as user]
            [version-tracker.storage :as storage]))

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

(s/defn router []
  (router/router
   [["/" {:get (wrap-basic-authentication handle-hello)}]
    ["/users" {:post signup/handle-signup}]]))

(def default-handler (router/create-default-handler
                      {:not-found (constantly {:status 404 :body "Not Found"})}))

(defn handler []
  (router/ring-handler (router) default-handler))

(defn wrap-print-exceptions
  "Prints exceptions thrown by handler. Useful for debugging tests."
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (println e)
        (throw e)))))

(defn app []
  (-> (handler)
      wrap-print-exceptions
      (wrap-defaults api-defaults)))
