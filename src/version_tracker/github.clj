(ns version-tracker.github
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clj-http.client :as http]
            [schema.core :as s]
            [version-tracker.config :as config]
            [version-tracker.crypto :as crypto]
            [version-tracker.release-client :as release-client]
            [version-tracker.model.user :as user]))

(s/defn ^:private send-graphql-request :- {s/Any s/Any}
  [base-url :- s/Str
   token :- s/Str
   query :- s/Str
   variables :- {s/Any s/Any}]
  (let [resp (http/post base-url
                        {:body (json/write-str {:query query
                                                :variables variables})
                         :headers {"authorization" (format "token %s" token)}})
        {:keys [data errors]} (-> resp :body (json/read-str :key-fn keyword))]
    (if (nil? data)
      (throw (ex-info "GraphQL error" {:query query
                                       :variables variables
                                       :errors errors}))
      data)))

(def repo-query (-> (io/resource "graphql/repo.graphql") slurp str/trim))

(def repo-summaries-query (-> (io/resource "graphql/repo_summaries.graphql") slurp str/trim))

(defrecord Client [config decrypter token]
  release-client/ReleaseClient
  (-get-repo-id [_this owner name]
    (let [data (send-graphql-request (:base-url config)
                                     token
                                     repo-query
                                     {:owner owner, :name name})]
      (-> data :repository :id)))

  (-get-repo-summaries [_this repo-ids]
    (let [data (send-graphql-request (:base-url config)
                                     token
                                     repo-summaries-query
                                     {:ids repo-ids})]
      (->> (:nodes data)
           (filter some?)
           (map (fn [node]
                  (let [owner-name (get-in node [:owner :login])
                        repo-name (:name node)
                        latest-release {:version (-> node :releases :nodes first :tagName)
                                        :date (-> node :releases :nodes first :publishedAt)}]
                    {:owner owner-name
                     :name repo-name
                     :latest-release latest-release})))))))

(s/defn add-credentials :- Client
  "Adds credentials to the github client."
  [github-client :- Client
   encrypted-token :- crypto/Bytes]
  (let [decrypter (:decrypter github-client)
        token (crypto/decrypt decrypter encrypted-token)]
    (assoc github-client :token token)))

(s/defn partial-client :- Client
  "Constructs a partial github client. Note that the client cannot be
  used until credentials are added."
  [config :- config/GitHub
   decrypter :- (s/protocol crypto/Decrypter)]
  (map->Client {:config config
                :decrypter decrypter}))

(s/defn wrap-client
  "Adds the github client to the request map. Requires basic-authentication
  middleware in outer layer."
  [handler
   client :- Client]
  (fn [request]
    (let [encrypted-token (-> request
                              :basic-authentication
                              ::user/encrypted-github-token)
          client' (add-credentials client encrypted-token)]
      (handler (assoc request ::client client')))))

(s/defn client-from-request :- Client
  "Extracts the client from the request map"
  [request]
  (::client request))
