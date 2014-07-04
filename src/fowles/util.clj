(ns fowles.util
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [zeromq.zmq :as zmq]
            [clojure.core.async :refer [>!! alt!! timeout]]
            [clj-time
             [core :as t]
             [coerce :as c]
             ]))

;;-------------------------------
;; async

(defn- get-n-in-ms
  "grab up n items from in-ch within max-ms."
  [in-ch n max-ms]
  (let [t (timeout max-ms)]
    (loop [i 0, acc []]
      (if (= i n)
        acc
        (alt!!
         t     ([_] acc)
         in-ch ([v] (recur (inc i) (conj acc v))))))))

(defn pipe-groups-of-up-to-n
  "grab up to num items from from in-ch and return as one msg on out-ch"
  [in-ch out-ch num max-wait-ms]
  (while true
    (let [vs (get-n-in-ms in-ch num max-wait-ms)]
      (if (> (count vs) 0)
        (>!! out-ch vs)))))

;;-------------------------------
;; ZMQ

(defn mk-connect-addr
  [host port]
  (str "tcp://" host ":" port))

(defn mk-socket
  ":: (keyword, str, int) -> zmq-socket"
  [push-or-pull host port]
  (let [context (zmq/context 1)]
    (doto (zmq/socket context push-or-pull)
      (zmq/connect (mk-connect-addr host port)))))

(def mk-pusher (partial mk-socket :push))
(def mk-puller (partial mk-socket :pull))

;;----------------------------

;; Why doesn't this work?
(defn prep-shutdown []
  (let [start-time (t/now)]
    (.addShutdownHook
     (Runtime/getRuntime)
     (Thread. (fn []
                (let [elapsed (- (c/to-long t/now) (c/to-long start-time))]
                  (println "Shutting down...")
                  (println "elapsed time:" elapsed)))))))
