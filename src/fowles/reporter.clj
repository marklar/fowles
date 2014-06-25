(ns fowles.reporter
  (:require [zeromq.zmq :as zmq]))

(defn mk-pusher
  ":: int -> zmq-push-socket"
  [port]
  (let [context (zmq/context 1)]
    (doto (zmq/socket context :push)
      (zmq/bind (str "tcp://*:" port)))))

(defn send-seq
  ":: (zmq-push-socket, iSeq) -> ()"
  [pusher seq]
  (doseq [i seq]
    (zmq/send-str pusher (str i))))
