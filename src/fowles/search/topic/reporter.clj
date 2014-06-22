(ns fowles.search.topic.reporter
  "Thread dedicated to outputing responses."
  (:require [fowles.reporter :as reporter]
            [clojure.data.json :as json]))

(def CHANNEL_IDS_FILE_NAME "io/channel_ids.txt")

(defn- get-channel-ids
  [resp-body]
  (let [items (get resp-body "items")]
    (map #(-> % (get "snippet") (get "channelId")) items)))

(defn- output-channel-ids
  [uris-ch response]
  (let [uri       (-> response :opts :url)
        resp-body (json/read-str (:body response))]
    ;; (println "uri:" uri)
    ;; (println "body:" resp-body)
    (reporter/append-strs-to-file (get-channel-ids resp-body)
                                  CHANNEL_IDS_FILE_NAME)
    (reporter/queue-next-uri uris-ch uri resp-body)))

;;---------------------------

(defn report
  ":: (chan, chan) -> ()"
  [from-ch uri-ch]
  (reporter/report from-ch (partial output-channel-ids uri-ch)))
