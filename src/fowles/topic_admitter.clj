(ns fowles.topic-admitter
  "Take in whatever input is required to perform data collection.
   + Source: from wherever (zmq socket?).
   + Data:   video-ids, queries.
   + Output: Put for requester."
  ;; [com.keminglabs.zmq-async.core :refer [register-socket!]]
  (:require [fowles.admitter]
            [clj-time.core :as t]))

;; https://developers.google.com/youtube/v3/guides/searching_by_topic
(def FREEBASE_TOPICS
  ;; TODO: fill these in (by re-running fetch and extracting these).
  ["/m/02566hr" "/m/0256724" "/m/025b7q2"])

(defn- enq-topics
  ":: (DateTime, DateTime, chan) -> ()"
  [start-date end-date to-ch]
  (admitter/enq to-ch (map (fn [topic-id] {:topic-id topic-id
                                           :start-date start-date
                                           :end-date end-date})
                           FREEBASE_TOPICS)))

;;----------------------

;; -> {topic-id, start-date, end-date}
(defn admit-topics []
  (let [start-date (t/date-time 2014 1 1)
        end-date   (t/date-time 2014 4 3)]
    (admitter/admit (partial enq-topics start-date end-date))))
