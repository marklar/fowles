(ns fowles.search.topic.searcher
  "Do YouTube Data API 'search'.
   Only for:
     + type 'video'
     + of a particular 'topicId'
     + publishedBefore a certain date.
   Get the channelId for each video and save it."
  (:require [clojure.core.async :refer [chan]]
            [fowles
             [cfg :as cfg]
             [requester :as requester]]
            [fowles.search.topic
             [admitter :as admitter]
             [uris :as uris]
             [reporter :as reporter]]))

(defn- search
  [api-key]
  (let [sleep-ch      (chan)
        topics-ch     (admitter/admit-topics)
        uris-ch       (uris/topic-search-uris api-key topics-ch)
        responses-ch  (requester/get-responses uris-ch sleep-ch)]
    (reporter/report responses-ch uris-ch sleep-ch))
  (while true))

(defn -main []
  (search (cfg/cfg-get :api-key)))
