(ns version-tracker.config
  (:require [aero.core :as aero]
            [clojure.java.io :as io]
            [schema.core :as s]))

(s/def Postgres
  {:host s/Str
   :port s/Int
   :database s/Str
   :user s/Str
   :password s/Str})

(s/def Config
  {:webserver {:port s/Num}
   :postgres Postgres})

(defn load-config []
  (let [config (aero/read-config (io/resource "config.edn"))]
    (s/validate Config config)
    config))
