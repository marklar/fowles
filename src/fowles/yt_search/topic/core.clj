(ns fowles.search.topic.core
  "Do YouTube Data API 'search'.
   Only for:
     + type 'video'
     + of a particular 'topicId'
     + 'publishedAfter' a certain date and 'publishedBefore' another
   Get the channelId for each video and save it."
  (:require [clojure.core.async :refer [chan]]
            [fowles
             [plumbing :as plumbing]]
            [fowles.search
             [reporter :as reporter]]
            [fowles.search.topic
             [cfg :as cfg]
             [admitter :as admitter]
             [uris :as uris]]))

(defn- mk-uris-ch []
  (let [topics-ch (admitter/admit-topics-from-file (cfg/in-file)
                                                   (cfg/start-date)
                                                   (cfg/end-date))
        uris-ch (uris/topic-search-uris (cfg/api-key)
                                        (cfg/part)
                                        (cfg/fields)
                                        topics-ch)]
    uris-ch))

(defn- search []
  (plumbing/report (mk-uris-ch)
                   (cfg/batch-size)
                   (cfg/frequency-ms)
                   (cfg/sleep-ms)
                   (cfg/failed-file)
                   (partial reporter/output-video-and-channel-ids
                            (cfg/out-file))))

(defn -main []
  (cfg/validate)
  (search)
  (while true))
