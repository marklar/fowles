(ns fowles.searcher
  (:require [fowles
             [cfg :as cfg]
             [admitter :as admitter]
             [requester :as requester]
             [gatherer :as gatherer]
             [reporter :as reporter]]
            [clj-time.core :as t]))

(defn- search
  [api-key]
  (->> (admitter/admit-query-words)
       (requester/request-searches api-key)
       gatherer/gather-responses
       reporter/report-search-result-ids)
  (while true))

(defn -main []
  (let [api-key (cfg/get-api-key)]
    (if (nil? api-key)
      (println "Missing API key.")
      (search api-key))))
