(ns fowles.uris
  (:refer-clojure :exclude [partition])
  (:require [clojure.string :as str]
            [cemerick.url :refer [url-encode]]
            [org.httpkit.client :as http]
            [clojure.core.async
             :refer [chan go >! pipe partition filter> map> map< alts!! to-chan]
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

(def PARTS_VIDEOS
  ["id" "snippet" "contentDetails" "fileDetails" "liveStreamingDetails"
   "player" "processingDetails" "recordingDetails" "statistics" "status"
   "suggestions" "topicDetails"])
;; (def PARTS_VIDEOS ["snippet" "contentDetails" "statistics" "status"])

(def IDS_PER_QUERY 50)

;; https://developers.google.com/youtube/v3/docs/videos/list
(defn- mk-video-uri
  ":: (str, [str]) -> str"
  [api-key video-ids]
  (let [args {:key api-key
              :id (csv video-ids)
              :part (csv PARTS_VIDEOS)}]
    (mk-uri "videos" args)))

;; Channels
(defn video-uris
  ":: (str, chan) -> chan"
  [api-key from-ch]
  (let [to-ch (->> (chan 1000)
                   ;; -> video-id
                   (async/partition IDS_PER_QUERY)
                   ;; -> [video-id]
                   (map< #(mk-video-uri api-key %)))]
    (pipe from-ch to-ch)
    to-ch))

;;---------------------------

;; (def MAX_RESULTS 50)
(def MAX_RESULTS 2)

(defn- longer? [n s]
  (>= (count s) n))

;; https://developers.google.com/youtube/v3/docs/search/list
(defn- mk-search-uri
  ":: (str, str) -> str"
  [api-key q]
  (let [args {:key api-key
              :type "video"
              :q (url-encode q)
              :maxResults MAX_RESULTS
              :order "viewCount"
              :part (csv ["id" "snippet"])}]
    (mk-uri "search" args)))

(defn search-uris
  ":: (str, chan) -> chan"
  [api-key from-ch]
  (let [to-ch (->> (chan 1000)
                   (filter> #(longer? 2 %))
                   (map> #(mk-search-uri api-key %)))]
    (pipe from-ch to-ch)
    to-ch))

;;-------------------------

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
  (let [to-ch (->> (chan 1000)
                   ;; -> {topic-id, start-date, end-date}
                   (map< #(mk-topic-search-uri api-key %)))]
    (pipe from-ch to-ch)
    to-ch))
