(defproject version-tracker "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [aero "1.1.6"]
                 [buddy/buddy-core "1.10.1"]
                 [buddy/buddy-hashers "1.8.1"]
                 [clj-http "3.12.3"]
                 [clojure.java-time "0.3.3"]
                 [com.layerware/hugsql "0.5.1"]
                 [com.stuartsierra/component "1.0.0"]
                 [hikari-cp "2.13.0"]
                 [metosin/reitit-ring "0.5.15"]
                 [org.apache.logging.log4j/log4j-api "2.17.1"]
                 [org.apache.logging.log4j/log4j-core "2.17.1"]
                 [org.clojure/data.json "2.4.0"]
                 [org.clojure/java.jdbc "0.7.12"]
                 [org.clojure/tools.logging "1.2.4"]
                 [org.postgresql/postgresql "42.3.1"]
                 [prismatic/schema "1.2.0"]
                 [ring "1.9.4"]
                 [ring/ring-defaults "0.3.2"]
                 [ring/ring-json "0.5.1"]
                 [ring-basic-authentication "1.1.1"]]
  :jvm-opts ["-Dclojure.tools.logging.factory=clojure.tools.logging.impl/log4j2-factory"]
  :main version-tracker.main
  :profiles
  {:dev {:dependencies [[com.cemerick/url "0.1.1"]]}})
