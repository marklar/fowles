(ns fowles.search.topic.reporter
  "Thread dedicated to outputing responses."
  (:require [fowles.reporter :as reporter]
            [clojure.data.json :as json]))

(def CHANNEL_IDS_FILE_NAME "io/channel_ids.txt")

(defn- get-channel-ids
  ":: hmap -> [str]"
  [resp-body]
  (let [items (get resp-body "items")]
    (map #(-> % (get "snippet") (get "channelId")) items)))

(defn- output-channel-ids
  ":: hmap -> ()
   From response, output info."
  [resp-body]
  (let [lines (get-channel-ids resp-body)]
    (reporter/append-strs-to-file CHANNEL_IDS_FILE_NAME)))

;;---------------------------

(defn report
  ":: (chan, chan) -> ()"
  [responses-ch uris-ch sleep-ch]
  (reporter/report responses-ch uris-ch sleep-ch
                   output-channel-ids))
