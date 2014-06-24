(ns fowles.search.reporter
  "Thread dedicated to outputing responses."
  (:require [fowles.util :as util]
            [clojure.data.json :as json]))

(defn- get-ids
  ":: hmap -> str"
  [item]
  (let [v-id (-> item (get "id")      (get "videoId"))
        c-id (-> item (get "snippet") (get "channelId"))]
    (str v-id "\t" c-id)))

(defn- get-video-and-channel-ids
  ":: hmap -> [str]"
  [resp-body]
  (let [items (get resp-body "items")]
    (map get-ids items)))

;;---------------------------------

(defn output-video-and-channel-ids
  ":: hmap -> ()
   From response, output info."
  [out-file resp-body]
  (let [lines (get-video-and-channel-ids resp-body)]
    (util/append-strs-to-file lines out-file)))
