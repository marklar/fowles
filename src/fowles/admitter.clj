(ns fowles.admitter
  "Take in whatever input is required to perform data collection.
   + Source: from wherever (zmq socket?).
   + Data:   video-ids, queries.
   + Output: Put for requester."
  ;; [com.keminglabs.zmq-async.core :refer [register-socket!]]
  (:refer-clojure :exclude [partition])
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clj-time.core :as t]
            [clojure.core.async
             :refer [chan go >!! partition]
             :as async]))

(def BUFFER_SIZE 100000)

(defn- enq
  "Enqueue some fixed sequence of items to to-ch.
   :: (chan, ISeq) -> ()"
  [to-ch seq]
  (doseq [i seq]
    (>!! to-ch i)))
;; DO NOT CLOSE.  We want the channel pipeline to remain open
;; for "next page" urls.
;; (close! to-ch))

(defn- admit [enq-fn]
  ":: ((chan -> ()) -> chan"
  (let [to-ch (chan BUFFER_SIZE)]
    (.start (Thread. (enq-fn to-ch)))
    to-ch))

;;------------

;; (def VIDEO_IDS_FILE_NAME "io/video_ids.txt")
(def VIDEO_IDS_FILE_NAME "io/video_and_channel_ids.txt")

(defn- enq-video-ids
  ":: chan -> ()"
  [to-ch]
  (let [lines     (str/split-lines (slurp VIDEO_IDS_FILE_NAME))
        video-ids (map #(first (str/split % #"\t")) lines)]
    (enq to-ch (take BUFFER_SIZE video-ids))))

;;------------

(def WORDS_FILE "/usr/share/dict/words")
(def NUM_WORDS 5)

(defn- enq-query-words
  ":: chan -> ()"
  [to-ch]
  (let [words (str/split-lines (slurp WORDS_FILE))]
    (enq to-ch (take NUM_WORDS words))))
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

(defn- enq-topics
  ":: (DateTime, DateTime, chan) -> ()"
  [start-date end-date to-ch]
  (enq to-ch (map (fn [topic-id] {:topic-id topic-id
                                  :start-date start-date
                                  :end-date end-date})
                  FREEBASE_TOPICS)))

;;----------------------

;;
;; TODO
;; Use a timeout here to push whatever there is onto the channel
;; if there are fewer than IDS_PER_QUERY there.
;;

(def IDS_PER_QUERY 5)
(defn admit-video-ids
  ":: () -> chan"
  []
  (async/partition IDS_PER_QUERY
                   (admit enq-video-ids)))

(defn admit-query-words
  ":: () -> chan"
  []
  (admit enq-query-words))

;; -> {topic-id, start-date, end-date}
(defn admit-topics []
  (let [start-date (t/date-time 2014 1 1)
        end-date   (t/date-time 2014 4 3)]
    (admit (partial enq-topics start-date end-date))))
