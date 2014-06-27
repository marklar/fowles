(ns fowles.admitter
  "Take in whatever input is required to perform data collection.
   + Source: from wherever (zmq socket?).
   + Data:   video-ids, queries.
   + Output: Put for requester."
  (:require [zeromq.zmq :as zmq]
            [clojure.core.async :refer [chan >!!]]))

(def BUFFER_SIZE 1000)

(defn- mk-connect-addr
  [host port]
  (str "tcp://" host ":" port))

(defn- mk-puller
  [ctx addr]
  (doto (zmq/socket ctx :pull)
    (zmq/connect addr)))

(defn- enq-from-puller
  [host port to-ch]
  (let [ctx  (zmq/context)
        addr (mk-connect-addr host port)]
    (with-open [puller (mk-puller ctx addr)]
      (println "Ready to receive input from addr:" addr)
      (while true
        (let [msg (zmq/receive-str puller)]
          (println "receiving:" msg)
          ;; -blocking- put
          (>!! to-ch msg))))))

;;-------------------

(defn admit [enq-fn]
  ":: ((chan -> ()) -> chan"
  (let [to-ch (chan BUFFER_SIZE)]
    (.start (Thread. #(enq-fn to-ch)))
    to-ch))

(defn from-puller
  ":: int -> chan"
  [host port]
  (let [to-ch (chan BUFFER_SIZE)]
    (.start (Thread. #(enq-from-puller host port to-ch)))
    to-ch))
