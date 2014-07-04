(ns fowles.search.topic.admitter
  "Take in whatever input is required to perform data collection.
   + Source: from wherever (zmq socket?).
   + Data:   video-ids, queries.
   + Output: Put for requester."
  ;; [com.keminglabs.zmq-async.core :refer [register-socket!]]
  (:require [fowles
             [admitter :as admitter]
             [util :as util]]
            [clojure.string :as str]
            [clj-time.core :as t]
            [clojure.core.async :refer [>!! close!]]))

;; https://developers.google.com/youtube/v3/guides/searching_by_topic
(defn- enq-topic-ids-from-file
  ":: (str, DateTime, DateTime, chan) -> ()"
  [in-file start-date end-date to-ch]
  (util/line-by-line in-file
                     (fn [topic-id]
                       (>!! to-ch {:topic-id topic-id
                                   :start-date start-date
                                   :end-date end-date})))
  (close! to-ch))

;;----------------------

;; -> {topic-id, start-date, end-date}
(defn admit-topics-from-file
  [in-file start-date end-date]
  ;; (let [start-date (t/date-time 2014 1 1)
  ;;       end-date   (t/date-time 2014 4 3)]
  (admitter/admit (partial enq-topic-ids-from-file in-file
                           start-date end-date)))
