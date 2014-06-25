(ns fowles.admitter
  "Take in whatever input is required to perform data collection.
   + Source: from wherever (zmq socket?).
   + Data:   video-ids, queries.
   + Output: Put for requester."
  (:require [zeromq.zmq :as zmq]
            [clojure.core.async :refer [go >! chan >!!]]))

(def BUFFER_SIZE 1000)

;; TODO: rename to enq-seq
(defn enq
  "Enqueue some fixed sequence of items to to-ch.
   :: (chan, ISeq) -> ()"
  [to-ch seq]
  (doseq [i seq]
    (>!! to-ch i)))
;; DO NOT CLOSE.  We want the channel pipeline to remain open
;; for "next page" urls.
;; (close! to-ch))

(defn- mk-puller
  [ctx addr]
  (doto (zmq/socket ctx :pull)
    (zmq/bind addr)))

(defn- enq-from-puller
  [port to-ch]
  (let [ctx (zmq/context)
        addr (str "tcp://*:" port)]
    (with-open [puller (mk-puller ctx addr)]
      (println "Ready to receive input on addr:" addr)
      (loop []
        (let [msg (zmq/receive-str puller)]
          (println "receiving:" msg)
          (go (>! to-ch msg)))
        (recur)))))

;;-------------------

(defn admit [enq-fn]
  ":: ((chan -> ()) -> chan"
  (let [to-ch (chan BUFFER_SIZE)]
    (.start (Thread. #(enq-fn to-ch)))
    to-ch))

(defn from-puller
  ":: int -> chan"
  [port]
  (let [to-ch (chan BUFFER_SIZE)]
    (.start (Thread. #(enq-from-puller port to-ch)))
    to-ch))
