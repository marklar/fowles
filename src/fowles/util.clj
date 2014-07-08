(ns fowles.util
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [zeromq.zmq :as zmq]
            [clojure.core.async :refer [>!! alt!! timeout close!]]
            [clj-time
             [core :as t]
             [coerce :as c]
             ]))

;;-------------------------------
;; async

(defn- maybe-send-acc
  [out-ch acc]
  (if (> (count acc) 0)  ;; any?
    (>!! out-ch acc)))

(defn- get-n-in-ms
  "Grab and send along up n items from in-ch within max-ms.
   Return bool: whether to continue."
  [in-ch n max-ms out-ch]
  (let [t (timeout max-ms)]
    (loop [i 0, acc []]
      (if (= i n)
        (do
          (maybe-send-acc out-ch acc)
          true)
        (alt!!
         t     ([_]
                  (maybe-send-acc out-ch acc)
                  true)
         in-ch ([v]
                  (if (nil? v)
                    (do
                      (maybe-send-acc out-ch acc)
                      (close! out-ch)
                      false)
                    (recur (inc i) (conj acc v)))))))))

(defn pipe-groups-of-up-to-n
  "grab up to num items from from in-ch and return as one msg on out-ch"
  [in-ch out-ch num max-wait-ms]
  (loop [continue true]
    (if continue
      (let [c (get-n-in-ms in-ch num max-wait-ms out-ch)]
        (recur c)))))

(defn mk-grouped-ch
  [in-ch size max-wait-ms]
  ;; Instead of starting a thread here, use `go`.
  (let [out-ch (chan)]
    (.start
     (Thread.
      #(pipe-groups-of-up-to-n in-ch out-ch size max-wait-ms)))
    out-ch))
  ;; (let [out-ch (chan)]
  ;;   (go (pipe-groups-of-up-to-n in-ch out-ch size wait-ms))
  ;;   out-ch))

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

