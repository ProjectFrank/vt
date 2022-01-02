(ns version-tracker.test-utils
  (:require [clojure.java.jdbc :as jdbc]
            [com.stuartsierra.component :as component]
            [schema.core :as s]
            [version-tracker.config :as config]
            [version-tracker.main :as main]
            [version-tracker.storage.sql :as sql]))

(defn schema-validation-fixture [t]
  (try
    (s/set-fn-validation! true)
    (t)
    (finally
      (s/set-fn-validation! false))))

(defmacro with-sql-storage
  "Runs body with a postgres-storage bound to symbol tx-sym. Database transactions are rolled back, not committed."
  [tx-sym & body]
  `(let [config# (config/load-config)
         sql-storage# (component/start
                       (sql/postgres-storage (:postgres config#)))]
    (try
      (jdbc/with-db-transaction [~tx-sym sql-storage#]
        (jdbc/db-set-rollback-only! ~tx-sym)
        ~@body)
      (finally
        (component/stop sql-storage#)))))

(defrecord RollbackStorage [sql-storage]
  component/Lifecycle
  (start [this]
    (component/start sql-storage))
  (stop [this]))

(defmacro with-system
  "Runs body in context of system bound to sys-sym. Database transactions are rolled back, not committed."
  [sys-sym & body]
  `(with-sql-storage tx#
     (let [~sys-sym (-> (main/system (config/load-config))
                        (assoc :storage tx#)
                        component/start)]
       (try
         ~@body
         (finally
           (component/stop ~sys-sym))))))
