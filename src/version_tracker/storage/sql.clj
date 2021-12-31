(ns version-tracker.storage.sql
  (:require [com.stuartsierra.component :as component]
            [hikari-cp.core :as cp]
            [schema.core :as s]
            [version-tracker.config :as config]))

(s/defrecord PostgresStorage [conn config :- config/Postgres]
  component/Lifecycle
  (start [this]
    (let [conn (cp/make-datasource {:server-name (:host config)
                                    :port-number (:port config)
                                    :database-name (:database config)
                                    :username (:user config)
                                    :password (:password config)
                                    :adapter "postgresql"})]
      (assoc this :conn {:datasource conn})))
  (stop [this]
    (cp/close-datasource conn)
    (dissoc this :conn)))

(s/defn postgres-storage [config :- config/Postgres]
  (map->PostgresStorage {:config config}))
