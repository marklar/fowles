(ns fowles.query-searcher
  (:require [fowles
             [cfg :as cfg]
             [admitter :as admitter]
             [uris :as uris]
             [requester :as requester]
             [gatherer :as gatherer]
             [query-reporter :as query-reporter]]))

(defn- search
  [api-key]
  (let [in->word          (admitter/admit-query-words)
        word->uri         (uris/search-uris api-key in->word)
        uri->promise      (requester/mk-promises word->uri)
        promise->response (gatherer/gather-responses uri->promise)]
    (query-reporter/report promise->response uri->promise))
  (while true))

(defn -main []
  (let [api-key (cfg/get-api-key)]
    (if (nil? api-key)
      (println "Missing API key.")
      (search api-key))))
