(ns version-tracker.server
  (:require [com.stuartsierra.component :as component]
            [ring.adapter.jetty :refer [run-jetty]]
            [version-tracker.release-client :as release-client]
            [version-tracker.storage :as storage]
            [version-tracker.crypto :as crypto]))

(defrecord Server [handler storage encrypter port]
  component/Lifecycle
  (start [this]
    (assoc this :server (run-jetty (-> handler
                                       (storage/wrap-storage storage))
                                   {:port port
                                    :join? false})))
  (stop [this]
    (.stop (:server this))
    this))

(defn new [port]
  (map->Server {:port port}))
