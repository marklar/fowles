(ns fowles.fetch.uris
  (:require [fowles.uris :as uris]
            [clojure.core.async :refer [chan pipe map<]]))

;; TODO: rename to 'requests'?

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
  [part fields video-ids]
  {:query-type "videos"
   :args {:id video-ids
          :part part
          :fields fields}})

(defn video-requests
  ":: (chan, [str], str) -> chan"
  [from-ch part fields]
  (let [->req (partial mk-request part fields)
        to-ch (map< ->req (chan))]
    ;; DO NOT CLOSE CHAN.
    (pipe from-ch to-ch false)
    to-ch))
