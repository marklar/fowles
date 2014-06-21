(ns fowles.query-reporter
  "Thread dedicated to outputing responses."
  (:require [fowles.reporter :as reporter]
            [clojure.data.json :as json]))

(def IDS_FILE_NAME "io/video_and_channel_ids.txt")

(defn- get-video-and-channel-ids
  [resp-body]
  (let [items   (get resp-body "items")
        get-ids (fn [item]
                  (let [v-id (-> item (get "id")      (get "videoId"))
                        c-id (-> item (get "snippet") (get "channelId"))]
                    (str v-id "\t" c-id)))]
    (map get-ids items)))

(defn- output-video-and-channel-ids
  [uris-ch response]
  (let [uri       (-> response :opts :url)
        resp-body (json/read-str (:body response))]
    (reporter/append-strs-to-file (get-video-and-channel-ids resp-body)
                                  IDS_FILE_NAME)
    (reporter/queue-next-uri uris-ch uri resp-body)))

;;------------------------------

(defn report
  ":: (chan, chan) -> ()"
  [from-ch uri-ch]
  (reporter/report from-ch (partial output-video-and-channel-ids uri-ch)))
