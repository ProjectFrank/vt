(ns version-tracker.main
  (:require [com.stuartsierra.component :as component]
            [version-tracker
             [server :as server]
             [handler :as handler]])
  (:gen-class))

(defn system []
  (component/system-map
   :server (component/using (server/new 3000)
                            [:handler])
   :handler handler/app))

(defn -main [& _args]
  (component/start (system)))
