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
             [plumbing :as plumbing]]
            [fowles.search.topic
             [admitter :as admitter]
             [uris :as uris]
             [reporter :as reporter]]))

(defn- mk-uris-ch
  [api-key]
  (let [topics-ch (admitter/admit-topics)
        uris-ch (uris/topic-search-uris api-key topics-ch)]
    uris-ch))

(defn- search
  [api-key]
  (plumbing/report (mk-uris-ch api-key)
                   reporter/output-channel-ids))

(defn -main []
  (search (cfg/cfg-get :api-key))
  (while true))
