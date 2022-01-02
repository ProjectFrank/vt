(ns version-tracker.handler
  (:require [reitit.ring :as router]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [version-tracker.handler.signup :as signup]))

(defn handle-hello [_]
  {:status 200 :body "Hello World" :headers {"Content-Type" "text/html"}})

(def router (router/router
             [["/" {:get handle-hello}]
              ["/users" {:post signup/handle-signup}]]))

(def default-handler (router/create-default-handler
                      {:not-found (constantly {:status 404 :body "Not Found"})}))

(def handler (router/ring-handler router default-handler))

(defn wrap-print-exceptions [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (println e)
        (throw e)))))

(def app (-> handler
             wrap-print-exceptions
             (wrap-defaults api-defaults)))
