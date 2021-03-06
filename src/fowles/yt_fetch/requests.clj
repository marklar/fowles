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

(def multi-res-requests #{:activities :playlistItems})

(defn- get-max-results
  [query-type]
  (if (contains? multi-res-requests query-type)
    50
    nil))

;;
;; https://developers.google.com/youtube/v3/docs/videos/list
;;
(defn- mk-request
  [query-type id-name part fields msg]
  {:query-type query-type
   :id-name id-name
   :args {id-name     (get msg id-name)
          :maxResults (get-max-results query-type)
          :part       part
          :fields     fields}})

(defn get-requests-ch
  ":: (keyword, chan, [str], str) -> chan"
  [query-type ids-ch id-name part fields]
  (map< (partial mk-request query-type id-name part fields) ids-ch))
