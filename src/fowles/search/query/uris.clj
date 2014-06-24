(ns fowles.search.query.uris
  (:require [fowles.uris :as uris]
            [clojure.core.async
             :refer [chan pipe filter> map<]
             :as async]))


(def MAX_RESULTS 50)
(def BUFFER_SIZE 1000)
(def TOO_SHORT 2)

(defn- longer? [n s]
  (>= (count s) n))

(def FIELDS
  (str "nextPageToken,"
       "items("
         "id(videoId),"
         "snippet(channelId)"
       ")"))

;; https://developers.google.com/youtube/v3/docs/search/list
(defn- mk-search-uri
  ":: (str, [str], str, {str, DateTime|str, DateTime|str}) -> str"
  [api-key part fields
   {:keys [query start-date end-date]}]
  (let [args {:key api-key
              :type "video"
              :maxResults MAX_RESULTS
              :order "viewCount"
              :q query
              :publishedAfter start-date
              :publishedBefore end-date
              :part part
              :fields fields}]
    (uris/mk-uri "search" args)))

;;-----------------------------

(defn search-uris
  ":: (str, chan) -> chan"
  [api-key part fields from-ch]
  (let [to-ch (->> (chan BUFFER_SIZE)
                   (filter> #(longer? TOO_SHORT %))
                   (map< #(mk-search-uri api-key part fields %)))]
    ;; DO NOT CLOSE
    (pipe from-ch to-ch false)
    to-ch))
