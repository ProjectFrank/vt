(ns version-tracker.server
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [com.stuartsierra.component :as component]))

(defrecord Server [handler port]
  component/Lifecycle
  (start [this]
    (assoc this :server (run-jetty handler {:port port
                                           :join? false})))
  (stop [this]
    (.stop (:server this))
    this))

(defn new [port]
  (map->Server {:port port}))
