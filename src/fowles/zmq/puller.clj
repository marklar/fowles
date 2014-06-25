(ns fowles.zmq.puller
  (:require [zeromq.zmq :as zmq]
            [clojure.core.async :refer [chan alts!!]]))

(def address "tcp://*:5557")

(defn mk-puller []
  (let [context (zmq/context 1)]
    (doto (zmq/socket context :pull)
      (zmq/bind address))))

(defn recv-str
  [sock]
  (String. (zmq/receive sock)))

;;------------

(defn -main []
  (with-open [puller (mk-puller)]
    (loop []
      (println (recv-str puller))
      (recur))))
