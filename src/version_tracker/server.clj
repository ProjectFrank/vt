(ns version-tracker.server
  (:require [com.stuartsierra.component :as component]
            [ring.adapter.jetty :refer [run-jetty]]
            [version-tracker.storage :as storage]))

(defrecord Server [handler storage port]
  component/Lifecycle
  (start [this]
    (assoc this :server (run-jetty (storage/wrap-storage handler storage)
                                   {:port port
                                    :join? false})))
  (stop [this]
    (.stop (:server this))
    this))

(defn new [port]
  (map->Server {:port port}))
