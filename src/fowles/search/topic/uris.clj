(ns fowles.search.topic.uris
  (:require [fowles.uris :as uris]
            [clojure.core.async
             :refer [chan pipe map<]
             :as async]))

;; TODO: Add 'fields' parameter.

;; https://developers.google.com/youtube/v3/docs/search/list
(defn- mk-topic-search-uri
  ":: (str, {str, DateTime, DateTime}) -> str"
  [api-key
   {:keys [topic-id start-date end-date]}]
  (let [args {:key api-key
              :type "video"
              :topicId topic-id
              :publishedAfter start-date
              :publishedBefore end-date
              :maxResults MAX_RESULTS
              :order "date"
              :part ["id" "snippet"]}]
    (uris/mk-uri "search" args)))

;;-------------------------------

(defn topic-search-uris
  ":: (str, chan) -> chan"
  [api-key from-ch]
  (let [to-ch (map< #(mk-topic-search-uri api-key %) (chan 1000))]
        ;; -> {topic-id, start-date, end-date}
    ;; DO NOT CLOSE
    (pipe from-ch to-ch false)
    to-ch))
