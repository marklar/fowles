(ns fowles.uris
  (:require [clojure.string :as str]))

;;--------------------------------

(def BASE_URI "https://www.googleapis.com/youtube/v3/videos")
;; (def PARTS
;;   ["id" "snippet" "contentDetails" "fileDetails" "liveStreamingDetails"
;;    "player" "processingDetails" "recordingDetails" "statistics" "status"
;;    "suggestions" "topicDetails"])
(def PARTS ["snippet" "contentDetails" "statistics" "status"])

;;--------------------------------

(defn- csv [strs]
  (str/join "," strs))

(defn- hmap->query-string [hmap]
  (->> hmap
       clojure.walk/stringify-keys
       (map (fn [[k v]] (str k "=" v)))
       (str/join "&")))

;;--------------------------------

;; https://developers.google.com/youtube/v3/docs/videos/list?hl=ca
(defn mk-video-uri
  [api-key video-ids]
  (let [args {:key api-key
              :id (csv video-ids)
              :part (csv PARTS)}]
    (str BASE_URI "?" (hmap->query-string args))))
