(ns fowles.uris
  (:require [clojure.string :as str]
            [cemerick.url :refer [url-encode]]))

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

;;--------------------------------

;; (def PARTS_VIDEOS
;;   ["id" "snippet" "contentDetails" "fileDetails" "liveStreamingDetails"
;;    "player" "processingDetails" "recordingDetails" "statistics" "status"
;;    "suggestions" "topicDetails"])
(def PARTS_VIDEOS ["snippet" "contentDetails" "statistics" "status"])

;; https://developers.google.com/youtube/v3/docs/videos/list
(defn mk-video-uri
  ":: (str, [str]) -> str"
  [api-key video-ids]
  (let [args {:key api-key
              :id (csv video-ids)
              :part (csv PARTS_VIDEOS)}]
    (str BASE_URI "/videos?" (hmap->query-string args))))

;;--------------------------------

;; https://developers.google.com/youtube/v3/docs/search/list
(defn mk-search-uri
  ":: (str, str) -> str"
  [api-key q]
  (let [args {:key api-key
              :q (url-encode q)
              :maxResults 50
              :type "video"
              :order "viewCount"
              :part (csv ["id" "snippet"])}]
    (str BASE_URI "/search?" (hmap->query-string args))))
