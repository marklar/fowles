(ns fowles.topic-searcher
  "Do YouTube Data API 'search'.
   Only for:
     + type 'video'
     + of a particular 'topicId'
     + publishedBefore a certain date.
   Get the channelId for those videos and save them."
  (:require [fowles
             [cfg :as cfg]
             [admitter :as admitter]
             [uris :as uris]
             [requester :as requester]
             [gatherer :as gatherer]
             [topic-reporter :as topic-reporter]]))

(defn- search
  [api-key]
  (let [in->topic         (admitter/admit-topics)
        topic->uri        (uris/topic-search-uris api-key in->topic)
        uri->promise      (requester/mk-promises topic->uri)
        promise->response (gatherer/gather-responses uri->promise)]
    (topic-reporter/report promise->response uri->promise))
  (while true))

(defn -main []
  (let [api-key (cfg/get-api-key)]
    (if (nil? api-key)
      (println "Missing API key.")
      (search api-key))))
