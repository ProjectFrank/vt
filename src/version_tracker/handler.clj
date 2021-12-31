(ns version-tracker.handler
  (:require [reitit.ring :as router]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))

(defn handle-hello [_]
  {:status 200 :body "Hello World" :headers {"Content-Type" "text/html"}})

(def router (router/router
             ["/" {:get handle-hello}]))

(def default-handler (router/create-default-handler
                      {:not-found (constantly {:status 404 :body "Not Found"})}))

(def handler (router/ring-handler router default-handler))

(def app
  (wrap-defaults handler site-defaults))
