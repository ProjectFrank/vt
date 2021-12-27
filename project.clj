(defproject version-tracker "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [aero "1.1.6"]
                 [compojure "1.6.1"]
                 [prismatic/schema "1.2.0"]
                 [ring "1.9.4"]
                 [ring/ring-defaults "0.3.2"]
                 [com.stuartsierra/component "1.0.0"]]
  :main version-tracker.main
  :plugins []
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.2"]]}})
