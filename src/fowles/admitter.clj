(ns fowles.admitter
  "Take in whatever input is required to perform data collection.
   + Source: from wherever (zmq socket?).
   + Data:   video-ids, queries.
   + Output: Put for requester."
  ;; [com.keminglabs.zmq-async.core :refer [register-socket!]]
  (:require [clojure.java.io :as io]
            [clj-time.core :as t]
            [clojure.core.async :refer [chan go >!!]]))

(def BUFFER_SIZE 100000)

(defn- enqueue
  "Enqueue some fixed sequence of items to to-ch.
   :: (chan, ISeq) -> ()"
  [to-ch seq]
  (doseq [i seq]
    (>!! to-ch i)))
;; DO NOT CLOSE.  We want the channel pipeline to remain open
;; for "next page" urls.
;; (close! to-ch))

(defn- admit [enqueue-fn]
  ":: ((chan -> ()) -> chan"
  (let [to-ch (chan BUFFER_SIZE)]
    (.start (Thread. (enqueue-fn to-ch)))
    to-ch))

;;------------

(def VIDEO_IDS
  ["7lCDEYXw3mM" "MjtOzLfebgY" "6QIw1BQIvT4" "2xJWQPdG7jE"])
(def all-video-ids
  (take 100 (cycle VIDEO_IDS)))

(def VIDEO_IDS_FILE_NAME "video_ids.txt")

(defn- enqueue-video-ids
  ":: chan -> ()"
  [to-ch]
  ;; (enqueue to-ch all-video-ids))
  (let [video-ids (clojure.string/split-lines (slurp VIDEO_IDS_FILE_NAME))]
    (enqueue to-ch (take BUFFER_SIZE video-ids))))

;;------------

(def WORDS_FILE "/usr/share/dict/words")

(defn- enqueue-query-words
  ":: chan -> ()"
  [to-ch]
  (let [words (clojure.string/split-lines (slurp WORDS_FILE))]
    (enqueue to-ch (take 20 words))))
  ;; (with-open [rdr (io/reader WORDS_FILE)]
  ;;   (doseq [word (line-seq rdr)]
  ;;     (println word)
  ;;     (>!! to-ch word))
  ;;   (close! to-ch)))

;;------------

;; https://developers.google.com/youtube/v3/guides/searching_by_topic
(def FREEBASE_TOPICS
  ;; TODO: fill these in (by re-running fetch and extracting these).
  ["/m/02566hr" "/m/0256724" "/m/025b7q2"])

(defn- enqueue-topics
  ":: (DateTime, DateTime, chan) -> ()"
  [start-date end-date to-ch]
  (enqueue to-ch (map (fn [topic-id] {:topic-id topic-id
                                      :start-date start-date
                                      :end-date end-date})
                      FREEBASE_TOPICS)))

;;----------------------

(defn admit-video-ids
  ":: () -> chan"
  []
  (admit enqueue-video-ids))

(defn admit-query-words
  ":: () -> chan"
  []
  (admit enqueue-query-words))

;; -> {topic-id, start-date, end-date}
(defn admit-topics []
  (let [start-date (t/date-time 2014 1 1)
        end-date   (t/date-time 2014 4 3)]
    (admit (partial enqueue-topics start-date end-date))))
