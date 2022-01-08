(ns version-tracker.main
  (:require [com.stuartsierra.component :as component]
            [schema.core :as s]
            [version-tracker.config :as config]
            [version-tracker.crypto :as crypto]
            [version-tracker.github :as github]
            [version-tracker.handler :as handler]
            [version-tracker.server :as server]
            [version-tracker.storage.sql :as sql])
  (:gen-class))

(s/defn system [config :- config/Config]
  (let [{:keys [encrypter]} (crypto/new-block-cipher (:crypto config))]
   (component/system-map
    :config config
    :release-client (github/github-client (:github config))
    :server (component/using
             (server/new
              (get-in config [:webserver :port]))
             [:handler :storage :release-client])
    :storage (component/using
              (sql/postgres-storage (:postgres config))
              [:encrypter])
    :handler (handler/app)
    :encrypter encrypter)))

(defn -main [& _args]
  (let [config (config/load-config)]
    (component/start (system config))))
