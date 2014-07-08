(ns fowles.util
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [zeromq.zmq :as zmq]
            [clojure.core.async :refer [chan close!
                                        go-loop <! >! alt! timeout]]
            [clj-time
             [core :as t]
             [coerce :as c]
             ]))

;;-------------------------------
;; async

(defn- get-n-in-ms
  "Grab up to n items from in-ch within max-wait-ms.
   Return [acc, bool]  (whether to continue)"
  [in-ch n max-wait-ms]
  (let [t (timeout max-wait-ms)]
    (go-loop [i 0, acc []]
             (if (= i n)
               ;; Got enough.
               [acc, true]
               ;; Maybe get more...
               (alt!
                t     ([_] [acc, true])
                in-ch ([v]
                         (if (nil? v)
                           [acc, false]
                           (recur (inc i) (conj acc v)))))))))

(defn mk-grouped-ch
  "grab up to num items from from in-ch and return as one msg on out-ch"
  [in-ch num max-wait-ms]
  (let [out-ch (chan)]
    (go-loop []
             (let [[acc c] (<! (get-n-in-ms in-ch num max-wait-ms))]
               (if (seq acc) (>! out-ch acc))
               (if c
                 (recur)
                 (close! out-ch))))
    out-ch))

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


(def SLEEP_SECS 5)

(defn prep-shutdown
  [msg-ch]
  (.addShutdownHook
   (Runtime/getRuntime)
   (Thread.
    (fn []
      ;; This stops any more input from coming in.
      (close! msg-ch)

      ;;
      ;; Join on all threads.  Each will terminate
      ;; if not receiving input for X seconds.
      ;; OR
      ;; Send a msg to each thread (via a channel)
      ;; telling it to stop what it's doing and send its
      ;; to-do items to the failure sink.
      ;;

      (println "\nSleeping for" SLEEP_SECS
               "seconds, to allow work to finish.")
      (loop [i 0]
        (if (< i SLEEP_SECS)
          (do
            (print ".")
            (flush)
            (Thread/sleep 1000)
            (recur (inc i)))
          (println "\nExiting.")))))))

