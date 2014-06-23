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
             [util :as util]
             [requester :as requester]
             [gatherer :as gatherer]]
            [fowles.search.topic
             [admitter :as admitter]
             [uris :as uris]
             [reporter :as reporter]]))

(defn- search
  [api-key]
  (let [sleep-ch      (chan)
        topics-ch     (admitter/admit-topics)
        uris-ch       (uris/topic-search-uris api-key topics-ch)
        responses-ch  (requester/get-responses uris-ch sleep-ch)
        bodies-ch     (gatherer/gather responses-ch uris-ch sleep-ch)]
    (util/report bodies-ch reporter/output-channel-ids))
  (while true))

(defn -main []
  (search (cfg/cfg-get :api-key)))
