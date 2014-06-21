(ns fowles.uris
  (:require [clojure.string :as str]
            [cemerick.url :refer [url-encode]]
            [org.httpkit.client :as http]
            [clojure.core.async
             :refer [chan go >! pipe filter> map< alts!! to-chan]
             :as async]))

(def BASE_URI "https://www.googleapis.com/youtube/v3")

;;--------------------------------

(defn- csv [strs]
  ":: [str] -> str"
  (str/join "," strs))

(defn- hmap->query-string
  ":: {keyword, str-able} -> str"
  [hmap]
  (->> hmap
       clojure.walk/stringify-keys
       (map (fn [[k v]] (str k "=" (str v))))
       (str/join "&")))

(defn- mk-uri
  [query-type args]
  (str BASE_URI "/" query-type "?" (hmap->query-string args)))

;;--------------------------------

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
              :id (csv video-ids)
              :part (csv PARTS_VIDEOS)
              :fields (url-encode FIELDS_VIDEOS)
              }]
    (mk-uri "videos" args)))

(defn video-uris
  ":: (str, chan) -> chan"
  [api-key from-ch]
  (let [to-ch (map< #(mk-video-uri api-key %) (chan 1000))]
    (pipe from-ch to-ch)
    to-ch))

;;---------------------------

(def MAX_RESULTS 50)
;; (def MAX_RESULTS 2)

(defn- longer? [n s]
  (>= (count s) n))

(def FIELDS_SEARCH
  (str "nextPageToken,"
       "items("
         "id(videoId),"
         "snippet(channelId)"
       ")"))

;; https://developers.google.com/youtube/v3/docs/search/list
(defn- mk-search-uri
  ":: (str, str) -> str"
  [api-key q]
  (let [args {:key api-key
              :type "video"
              :q (url-encode q)
              :maxResults MAX_RESULTS
              :order "viewCount"
              :part (csv ["id" "snippet"])
              :fields (url-encode FIELDS_SEARCH)
              }]
    (mk-uri "search" args)))

(defn search-uris
  ":: (str, chan) -> chan"
  [api-key from-ch]
  (let [to-ch (->> (chan 1000)
                   (filter> #(longer? 2 %))
                   (map< #(mk-search-uri api-key %)))]
    (pipe from-ch to-ch)
    to-ch))

;;-------------------------

;; TODO: Add 'fields' parameter.

;; https://developers.google.com/youtube/v3/docs/search/list
(defn- mk-topic-search-uri
  ":: (str, {str, DateTime, DateTime}) -> str"
  [api-key
   {:keys [topic-id start-date end-date]}]
  (let [args {:key api-key
              :type "video"
              :topicId (url-encode topic-id)
              :publishedAfter (url-encode (str start-date))
              :publishedBefore (url-encode (str end-date))
              :maxResults MAX_RESULTS
              :order "date"
              :part (csv ["id" "snippet"])}]
    (mk-uri "search" args)))

(defn topic-search-uris
  ":: (str, chan) -> chan"
  [api-key from-ch]
  (let [to-ch (map< #(mk-topic-search-uri api-key %) (chan 1000))]
        ;; -> {topic-id, start-date, end-date}
    (pipe from-ch to-ch)
    to-ch))
