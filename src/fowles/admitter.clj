(ns fowles.admitter
  "Take in whatever input is required to perform data collection.
   + Source: from wherever (zmq socket?).
   + Data:   video-ids, queries.
   + Output: Put for requester."
  (:require [clojure.core.async :refer [chan >!!] :as async]))

(def BUFFER_SIZE 100000)

(defn enq
  "Enqueue some fixed sequence of items to to-ch.
   :: (chan, ISeq) -> ()"
  [to-ch seq]
  (doseq [i seq]
    (>!! to-ch i)))
;; DO NOT CLOSE.  We want the channel pipeline to remain open
;; for "next page" urls.
;; (close! to-ch))

(defn admit [enq-fn]
  ":: ((chan -> ()) -> chan"
  (let [to-ch (chan BUFFER_SIZE)]
    (.start (Thread. (enq-fn to-ch)))
    to-ch))
