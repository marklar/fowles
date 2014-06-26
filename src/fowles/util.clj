(ns fowles.util
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [zeromq.zmq :as zmq]
            [clj-time
             [core :as t]
             [coerce :as c]
             ]))

(defn- mk-connect-addr
  [host port]
  (str "tcp://" host ":" port))

(defn- mk-socket
  ":: (keyword, str, int) -> zmq-socket"
  [push-or-pull host port]
  (let [context (zmq/context 1)]
    (doto (zmq/socket context push-or-pull)
      (zmq/connect (mk-connect-addr host port)))))

(def mk-pusher (partial mk-socket :push))
(def mk-puller (partial mk-socket :pull))

;;-------------------------------  

;; Why doesn't this work?
(defn prep-shutdown []
  (let [start-time (t/now)]
    (.addShutdownHook
     (Runtime/getRuntime)
     (Thread. (fn []
                (let [elapsed (- (c/to-long t/now) (c/to-long start-time))]
                  (println "Shutting down...")
                  (println "elapsed time:" elapsed)))))))
