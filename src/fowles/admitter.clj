(ns fowles.admitter
  "Take in whatever input is required to perform data collection.
   + Source: from wherever (zmq socket?).
   + Data:   video-ids, queries.
   + Output: Put for requester."
  ;; [com.keminglabs.zmq-async.core :refer [register-socket!]]
  (:require [clojure.core.async :refer [chan go >!! close!]]))

(defn- enqueue
  "Enqueue some fixed sequence of items to to-ch."
  [to-ch seq]
  (doseq [i seq]
    (>!! to-ch i))
  (close! to-ch))

(def VIDEO_IDS
  ["7lCDEYXw3mM" "MjtOzLfebgY" "6QIw1BQIvT4" "2xJWQPdG7jE"])

(def all-video-ids
  (take 100 (cycle VIDEO_IDS)))

(defn- enqueue-video-ids
  [to-ch]
  (enqueue to-ch all-video-ids))

(def WORDS_FILE "/usr/share/dict/words")

(defn- enqueue-query-words
  [to-ch]
  (let [words (clojure.string/split-lines (slurp WORDS_FILE))]
    (enqueue to-ch (take 20 words))))

(def BUFFER_SIZE 1000)

(defn- admit [enqueue-fn]
  (let [to-ch (chan BUFFER_SIZE)]
    (.start (Thread. (enqueue-fn to-ch)))
    to-ch))

;;----------------------

(defn admit-video-ids []
  (admit enqueue-video-ids))

(defn admit-query-words []
  (admit enqueue-query-words))
