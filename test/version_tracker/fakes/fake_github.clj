(ns version-tracker.fakes.fake-github
  (:require [cemerick.url :as url]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [ring.adapter.jetty :refer [run-jetty]]
            [schema.core :as s]
            [version-tracker.config :as config])
  (:import [org.eclipse.jetty.server Server]))

(def repo-query
  (str/trim "
query ($owner:String!, $name:String!) {
  repository(owner:$owner, name:$name) {
    id
  }
}
"))

(def repo-summaries-query
  (str/trim "
query($ids:[ID!]!) {
  nodes(ids:$ids) {
    ... on Repository {
      owner {
        login
      }
      name
      releases(first:1, orderBy:{field:CREATED_AT, direction:DESC}) {
        nodes {
          publishedAt
          tagName
        }
      }
    }
  }
}
"))

(def repo-github-id "MDEwOlJlcG9zaXRvcnk0MTg4MTkwMA==")

(def repo-response
  (format (str/trim "
{
  \"data\": {
    \"repository\": {
      \"id\": \"%s\"
    }
  }
}
")
          repo-github-id))

(def not-found-repo-response
  (str/trim "
{
  \"data\": {
    \"repository\": null
  },
  \"errors\": [
    {
      \"type\": \"NOT_FOUND\",
      \"path\": [
        \"repository\"
      ],
      \"locations\": [
        {
          \"line\": 7,
          \"column\": 3
        }
      ],
      \"message\": \"Could not resolve to a Repository with the name 'notfound/notfound'.\"
    }
  ]
}
"))

(def good-token "magic-token")

(defn- response [body]
  {:status 200, :body body, :headers {"content-type" "application/json"}})

(defn- mock-repo-query [variables]
  (if (= {:owner "microsoft", :name "vscode"} variables)
    (response repo-response)
    (response not-found-repo-response)))

(def repo-summaries-response
  (str/trim "
{
  \"data\": {
    \"nodes\": [
      {
        \"owner\": {
          \"login\": \"microsoft\"
        },
        \"name\": \"vscode\",
        \"releases\": {
          \"nodes\": [
            {
              \"name\": \"November 2021 Recovery 2\",
              \"publishedAt\": \"2021-12-16T17:51:28Z\",
              \"tagName\": \"1.63.2\"
            }
          ]
        }
      }
    ]
  }
}
"))

(defn- mock-repo-summaries-query [variables]
  (if (= variables {:ids ["MDEwOlJlcG9zaXRvcnk0MTg4MTkwMA=="]})
    (response repo-summaries-response)
    (let [body (json/write-str {:data
                                {:nodes
                                 (repeat (count (:ids variables)) nil)}})]
      (response body))))

(defn handler [path {:keys [request-method uri body headers]}]
  (if-not (and (= :post request-method)
               (= uri path))
    {:status 500 :body "Fake GitHub only handles requests to POST /graphql"}
    (let [{:keys [query variables]} (json/read-str (slurp body) :key-fn keyword)]
      (cond
        (not= (get headers "authorization")
              (format "token %s" good-token))
        {:status 401}

        (not (#{repo-query repo-summaries-query} query))
        {:status 500, :body (format "Unsupported graphql query: %s" query)}

        (= repo-query query)
        (mock-repo-query variables)

        (= repo-summaries-query query)
        (mock-repo-summaries-query variables)

        :else
        {:status 500, :body "unknown error"}))))

(s/defn start :- {:server Server
                  :base-url s/Str}
  []
  (let [config (config/load-config {:profile :test})
        base-url (-> config :github :base-url)
        url (url/url base-url)]
   {:server (run-jetty (partial handler (:path url))
                       {:join? false
                        :port (:port url)})
    :base-url base-url}))

(s/defn stop :- (s/eq nil)
  [^Server server]
  (.stop server))
