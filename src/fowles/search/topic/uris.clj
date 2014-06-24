(ns fowles.search.topic.uris
  (:require [fowles.uris :as uris]
            [clojure.core.async
             :refer [chan pipe map<]
             :as async]))

(def MAX_RESULTS 50)
(def BUFFER_SIZE 1000)

;; https://developers.google.com/youtube/v3/docs/search/list
(defn- mk-topic-search-uri
  ":: (str, [str], str, {str, DateTime|str, DateTime|str}) -> str"
  [api-key part fields
   {:keys [topic-id start-date end-date]}]
  (let [args {:key api-key
              :type "video"
              :maxResults MAX_RESULTS
              :order "date"
              :topicId topic-id
              :publishedAfter start-date
              :publishedBefore end-date
              :part part
              :fields fields}]
    (uris/mk-uri "search" args)))

;;-------------------------------

(defn topic-search-uris
  ":: (str, chan) -> chan"
  [api-key part fields from-ch]
  ;; Expect from-ch to provide messages as hmaps, like this:
  ;;     {topic-id, start-date, end-date}
  (let [to-ch (map< (partial mk-topic-search-uri api-key part fields)
                    (chan BUFFER_SIZE))]
    ;; DO NOT CLOSE
    (pipe from-ch to-ch false)
    to-ch))
