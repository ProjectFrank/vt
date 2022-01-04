(defproject version-tracker "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [aero "1.1.6"]
                 [buddy/buddy-hashers "1.8.1"]
                 [clj-http "3.12.3"]
                 [com.layerware/hugsql "0.5.1"]
                 [com.stuartsierra/component "1.0.0"]
                 [hikari-cp "2.13.0"]
                 [metosin/reitit-ring "0.5.15"]
                 [org.clojure/data.json "2.4.0"]
                 [org.clojure/java.jdbc "0.7.12"]
                 [org.postgresql/postgresql "42.3.1"]
                 [prismatic/schema "1.2.0"]
                 [ring "1.9.4"]
                 [ring/ring-defaults "0.3.2"]
                 [ring/ring-json "0.5.1"]
                 [ring-basic-authentication "1.1.1"]]
  :main version-tracker.main
  :plugins []
  :profiles
  {:dev {:dependencies []
         :source-paths ["src" "test"]}})
