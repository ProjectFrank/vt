(ns version-tracker.config
  (:require [aero.core :as aero]
            [clojure.java.io :as io]
            [schema.core :as s]))

(s/def Config {:webserver {:port s/Num}})

(defn load-config []
  (let [config (aero/read-config (io/resource "config.edn"))]
    (s/validate Config config)
    config))
