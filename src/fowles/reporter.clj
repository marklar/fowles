(ns fowles.reporter
  (:require [zeromq.zmq :as zmq]))

(defn send-seq
  ":: (zmq-push-socket, iSeq) -> ()"
  [pusher seq]
  (doseq [i seq]
    (zmq/send-str pusher (str i))))
