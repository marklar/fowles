(ns fowles.search.query.uris
  (:require [fowles.uris :as uris]
            [clojure.core.async
             :refer [chan pipe filter> map<]
             :as async]))


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
              :q q
              :maxResults MAX_RESULTS
              :order "viewCount"
              :part ["id" "snippet"]
              :fields FIELDS_SEARCH
              }]
    (uris/mk-uri "search" args)))

;;-----------------------------

(defn search-uris
  ":: (str, chan) -> chan"
  [api-key from-ch]
  (let [to-ch (->> (chan 1000)
                   (filter> #(longer? 2 %))
                   (map< #(mk-search-uri api-key %)))]
    ;; DO NOT CLOSE
    (pipe from-ch to-ch false)
    to-ch))
