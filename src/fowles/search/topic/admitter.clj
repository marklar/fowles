(ns fowles.search.topic.admitter
  "Take in whatever input is required to perform data collection.
   + Source: from wherever (zmq socket?).
   + Data:   video-ids, queries.
   + Output: Put for requester."
  ;; [com.keminglabs.zmq-async.core :refer [register-socket!]]
  (:require [fowles.admitter :as admitter]
            [clojure.string :as str]
            [clj-time.core :as t]))

;; https://developers.google.com/youtube/v3/guides/searching_by_topic
(defn- enq-topics-from-file
  ":: (DateTime, DateTime, chan) -> ()"
  [in-file start-date end-date to-ch]
  ;; FIXME: The problem: NOT lazy!
  (let [topic-ids (str/split-lines (slurp in-file))]
    (admitter/enq to-ch (map (fn [topic-id] {:topic-id topic-id
                                             :start-date start-date
                                             :end-date end-date})
                             topic-ids))))

;;----------------------

;; -> {topic-id, start-date, end-date}
(defn admit-topics-from-file
  [in-file start-date end-date]
  ;; (let [start-date (t/date-time 2014 1 1)
  ;;       end-date   (t/date-time 2014 4 3)]
  (admitter/admit (partial enq-topics-from-file in-file
                           start-date end-date)))
