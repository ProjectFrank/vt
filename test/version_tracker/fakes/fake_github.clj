(ns version-tracker.fakes.fake-github
  (:require [clojure.data.json :as json]
            [clojure.string :as str]
            [ring.adapter.jetty :refer [run-jetty]]
            [schema.core :as s])
  (:import [org.eclipse.jetty.server Server]))

(def repo-query
  (str/trim "
query ($owner:String!, $name:String!) {
  repository(owner:$owner, name:$name) {
    id
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

(defn handler [token {:keys [request-method uri body headers]}]
  (if-not (and (= :post request-method)
               (= uri "/graphql"))
    {:status 500 :body "Fake GitHub only handles requests to POST /graphql"}
    (let [{:keys [query variables]} (json/read-str (slurp body) :key-fn keyword)]
      (cond
        (not= (get headers "authorization")
              (format "token %s" token))
        {:status 401}

        (not= repo-query query)
        {:status 500, :body (format "Unsupported graphql query: %s" query)}

        (= {:owner "microsoft", :name "vscode"} variables)
        {:status 200, :body repo-response, :headers {"content-type" "application/json"}}

        (= {:owner "notfound", :name "notfound"} variables)
        {:status 200, :body not-found-repo-response, :headers {"content-type" "application/json"}}

        :else
        {:status 500, :body "unknown error"}))))

(s/defn start :- {:server Server
                  :base-url s/Str}
  [{:keys [port token]} :- {:port s/Int, :token s/Str}]
  {:server (run-jetty (partial handler token)
                      {:join? false
                       :port port})
   :base-url (format "http://localhost:%d/graphql" port)})

(s/defn stop :- (s/eq nil)
  [^Server server]
  (.stop server))
