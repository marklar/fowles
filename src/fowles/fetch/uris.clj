(ns fowles.fetch.uris
  (:require [fowles.uris :as uris]
            [clojure.core.async :refer [chan pipe map<]]))

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
(def PARTS
  ["contentDetails"  ;; 2
   "snippet"         ;; 2
   "statistics"      ;; 2
   "status"          ;; 2
   "topicDetails"    ;; 2
   ])

;; https://developers.google.com/youtube/v3/getting-started#fields
(def FIELDS
  (str 
   "items("
     "id,status,statistics,topicDetails,"
     "contentDetails(duration,licensedContent),"  ;; regionRestriction?
     "snippet(publishedAt,channelId,title,categoryId,liveBroadcastContent)"
   ")"))

;;
;; TODO: Make this more efficient.
;; Currently it's reconstructing the entire URL
;; with every set of video-ids.
;;
;; https://developers.google.com/youtube/v3/docs/videos/list
(defn- mk-video-uri
  ":: (str, [str]) -> str"
  [api-key part fields video-ids]
  (let [args {:key api-key
              :id video-ids
              :part part
              :fields fields
              }]
    (uris/mk-uri "videos" args)))

(defn video-uris
  ":: (chan, str, [str], str) -> chan"
  [from-ch api-key part fields]
  (let [->uri (partial mk-video-uri api-key part fields)
        to-ch (map< ->uri (chan))]
    ;; DO NOT CLOSE CHAN.
    (pipe from-ch to-ch false)
    to-ch))
