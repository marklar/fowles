(ns fowles.admitter
  "Take in whatever input is required to perform data collection.
   + Source: from wherever (zmq socket?).
   + Data:   video-ids, queries.
   + Output: Put for requester."
  ;; [com.keminglabs.zmq-async.core :refer [register-socket!]]
  (:require [clojure.java.io :as io]
            [clojure.core.async :refer [chan go >!! close!]]))

(def BUFFER_SIZE 100000)

(defn- enqueue
  "Enqueue some fixed sequence of items to to-ch."
  [to-ch seq]
  (doseq [i seq]
    (>!! to-ch i))
  (close! to-ch))

;;------------

(def VIDEO_IDS
  ["7lCDEYXw3mM" "MjtOzLfebgY" "6QIw1BQIvT4" "2xJWQPdG7jE"])
(def all-video-ids
  (take 100 (cycle VIDEO_IDS)))

(def VIDEO_IDS_FILE_NAME "video_ids.txt")

(defn- enqueue-video-ids
  [to-ch]
  ;; (enqueue to-ch all-video-ids))
  (let [video-ids (clojure.string/split-lines (slurp VIDEO_IDS_FILE_NAME))]
    (enqueue to-ch (take BUFFER_SIZE video-ids))))

;;------------

(def WORDS_FILE "/usr/share/dict/words")

(defn- enqueue-query-words
  [to-ch]
  (let [words (clojure.string/split-lines (slurp WORDS_FILE))]
    (enqueue to-ch (take 1000 words))))
  ;; (with-open [rdr (io/reader WORDS_FILE)]
  ;;   (doseq [word (line-seq rdr)]
  ;;     (println word)
  ;;     (>!! to-ch word))
  ;;   (close! to-ch)))

;;------------

(defn- admit [enqueue-fn]
  (let [to-ch (chan BUFFER_SIZE)]
    (.start (Thread. (enqueue-fn to-ch)))
    to-ch))

;;----------------------

(defn admit-video-ids []
  (admit enqueue-video-ids))

(defn admit-query-words []
  (admit enqueue-query-words))
