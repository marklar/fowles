(ns fowles.yt-fetch.requests
  (:require [clojure.core.async :refer [map<]]))

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
;; https://developers.google.com/youtube/v3/docs/videos/list
;;
(defn- mk-request
  [query-type id-name part fields msg]
  {:query-type query-type
   :args {id-name (get msg id-name)
          :part   part
          :fields fields}})

(defn get-requests-ch
  ":: (keyword, chan, [str], str) -> chan"
  [query-type ids-ch id-name part fields]
  (map< (partial mk-request query-type id-name part fields) ids-ch))
