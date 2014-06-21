(ns fowles.query-searcher
  (:require [fowles
             [cfg :as cfg]
             [requester :as requester]
             [gatherer :as gatherer]
             [query-admitter :as query-admitter]
             [query-uris :as query-uris]
             [query-reporter :as query-reporter]]))

(defn- search
  [api-key]
  (let [in->word          (query-admitter/admit-query-words)
        word->uri         (query-uris/search-uris api-key in->word)
        uri->promise      (requester/mk-promises word->uri)
        promise->response (gatherer/gather-responses uri->promise)]
    (query-reporter/report promise->response uri->promise))
  (while true))

(defn -main []
  (let [api-key (cfg/get-api-key)]
    (if (nil? api-key)
      (println "Missing API key.")
      (search api-key))))
