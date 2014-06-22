(ns fowles.search.query.core
  (:require [fowles
             [cfg :as cfg]
             [requester :as requester]
             [gatherer :as gatherer]]
            [fowles.search.query
             [admitter :as admitter]
             [uris :as uris]
             [reporter :as reporter]]))

(defn- search
  [api-key]
  (let [in->word          (admitter/admit-query-words)
        word->uri         (uris/search-uris api-key in->word)
        uri->promise      (requester/mk-promises word->uri)
        promise->response (gatherer/gather-responses uri->promise)]
    (reporter/report promise->response uri->promise))
  (while true))

(defn -main []
  (let [api-key (cfg/get-api-key)]
    (if (nil? api-key)
      (println "Missing API key.")
      (search api-key))))
