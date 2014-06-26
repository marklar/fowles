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

;;------------------------

;;
;; TODO: Rather than creating a new wrtr each time,
;; create one the first time and then reuse.
;;
;; Is one of these more efficient than the other?
;; (spit file-name
;;       (str (str/join "\n" strs) "\n")
;;       :append true))))
;;
(defn append-strs-to-file
  ":: ([str], str) -> ()"
  [strs file-name]
  (with-open [wrtr (io/writer file-name :append true)]
    (doseq [s strs] (.write wrtr (str s "\n")))))

(defn line-by-line
  [file-name line-fn]
  (with-open [rdr (io/reader file-name)]
    (doseq [line (line-seq rdr)]
      (line-fn line))))
