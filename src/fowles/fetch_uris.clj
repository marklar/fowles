(ns fowles.fetch-uris
  (:require [fowles.uris :as uris]
            [clojure.core.async
             :refer [chan pipe map<]
             :as async]))

;;
;; https://developers.google.com/youtube/v3/getting-started#part
;;
;; --- FORBIDDEN
;; fileDetails: 1
;; processingDetails: 1
;; suggestions: 1
;;
;; --- NOT INTERESTING
;; id: 0
;; liveStreamingDetails: 2
;; player: 0
;; recordingDetails: 2
;;
(def PARTS_VIDEOS
  ["contentDetails"  ;; 2
   "snippet"         ;; 2
   "statistics"      ;; 2
   "status"          ;; 2
   "topicDetails"    ;; 2
   ])

;; https://developers.google.com/youtube/v3/getting-started#fields
(def FIELDS_VIDEOS
  (str 
   "items("
     "id,status,statistics,topicDetails,"
     "contentDetails(duration,licensedContent),"  ;; regionRestriction?
     "snippet(publishedAt,channelId,title,categoryId,liveBroadcastContent)"
   ")"))

;; https://developers.google.com/youtube/v3/docs/videos/list
(defn- mk-video-uri
  ":: (str, [str]) -> str"
  [api-key video-ids]
  (let [args {:key api-key
              :id video-ids
              :part PARTS_VIDEOS
              :fields FIELDS_VIDEOS
              }]
    (uris/mk-uri "videos" args)))

(defn video-uris
  ":: (str, chan) -> chan"
  [api-key from-ch]
  (let [to-ch (map< #(mk-video-uri api-key %) (chan 1000))]
    (pipe from-ch to-ch)
    to-ch))
