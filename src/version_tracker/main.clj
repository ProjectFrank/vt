(ns version-tracker.main
  (:require [com.stuartsierra.component :as component]
            [version-tracker
             [config :as config]
             [handler :as handler]
             [server :as server]])
  (:gen-class))

(defn system [config]
  (component/system-map
   :server (component/using
            (server/new
             (get-in config [:webserver :port]))
            [:handler])
   :handler handler/app))

(defn -main [& _args]
  (let [config (config/load-config)]
    (component/start (system config))))
