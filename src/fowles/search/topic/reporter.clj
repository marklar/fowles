(ns fowles.search.topic.reporter
  "Thread dedicated to outputing responses."
  (:require [fowles.util :as util]
            [clojure.data.json :as json]))

(def CHANNEL_IDS_FILE_NAME "io/channel_ids.txt")

(defn- get-channel-ids
  ":: hmap -> [str]"
  [resp-body]
  (let [items (get resp-body "items")]
    (map #(-> % (get "snippet") (get "channelId")) items)))

;;---------------------------------

(defn output-channel-ids
  ":: hmap -> ()
   From response, output info."
  [resp-body]
  (let [lines (get-channel-ids resp-body)]
    (util/append-strs-to-file CHANNEL_IDS_FILE_NAME)))
