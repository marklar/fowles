(ns fowles.zmq.pusher
  (:require [zeromq.zmq :as zmq]
            [clojure.core.async :refer [chan alts!!]]
            ))

(def address "tcp://127.0.0.1:5557")

(defn mk-pusher
  ":: () -> zmq-push-socket"
  []
  (let [context (zmq/context 1)]
    (doto (zmq/socket context :push)
      (zmq/connect address))))

(defn send-seq
  ":: (zmq-push-socket, iSeq) -> ()"
  [pusher seq]
  (doseq [i seq]
    (zmq/send-str pusher (str i))))

;;------------

(defn -main []
  (with-open [pusher (mk-pusher)]
    (send-seq pusher (range 10))))
