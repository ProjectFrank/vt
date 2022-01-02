(ns version-tracker.main
  (:require [com.stuartsierra.component :as component]
            [schema.core :as s]
            [version-tracker.config :as config]
            [version-tracker.handler :as handler]
            [version-tracker.server :as server]
            [version-tracker.storage.sql :as sql])
  (:gen-class))

(s/defn system [config :- config/Config]
  (component/system-map
   :config config
   :server (component/using
            (server/new
             (get-in config [:webserver :port]))
            [:handler :storage])
   :storage (sql/postgres-storage (:postgres config))
   :handler handler/app))

(defn -main [& _args]
  (let [config (config/load-config)]
    (component/start (system config))))
