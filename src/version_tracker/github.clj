(ns version-tracker.github
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clj-http.client :as http]
            [schema.core :as s]
            [version-tracker.config :as config]
            [version-tracker.release-client :as release-client]))

(s/defn ^:private send-graphql-request :- {s/Any s/Any}
  [config :- config/GitHub
   query :- s/Str
   variables :- {s/Any s/Any}]
  (let [resp (http/post (:base-url config)
                        {:body (json/write-str {:query query
                                                :variables variables})
                         :headers {"authorization" (format "token %s" (:token config))}})
        {:keys [data errors]} (-> resp :body (json/read-str :key-fn keyword))]
    (if (nil? data)
      (throw (ex-info "GraphQL error" {:query query
                                       :variables variables
                                       :errors errors}))
      data)))

(def repo-query (-> (io/resource "graphql/repo.graphql") slurp str/trim))

(defrecord GitHubClient [config]
  release-client/ReleaseClient
  (-get-repo-id [_this owner name]
    (let [data (send-graphql-request config
                                     repo-query
                                     {:owner owner, :name name})]
      (-> data :repository :id))))
(s/defn github-client [config :- config/GitHub]
  (->GitHubClient config))
