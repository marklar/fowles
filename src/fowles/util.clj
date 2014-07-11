(ns fowles.util
  (:require [clojure.java.io :as io]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [zeromq.zmq :as zmq]
            [clojure.core.async :refer [chan close! timeout
                                        go-loop <! >! alt!
                                        <!! >!! alt!!]]
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

(def WAIT_MS 1000)

(defn dequeue-all-timeout
  [ch ms]
  (let [t (timeout ms)]
    (loop [acc []]
      (alt!!
       t  ([_] acc)
       ch ([v]
             (if (nil? v)
               acc
               (recur (conj acc v))))))))

(defn print-msgs
  [name ch]
  (let [msgs (dequeue-all-timeout ch WAIT_MS)]
    (json/pprint {name msgs})))

(defn request-msg->input-msg
  [request-msg]
  (let [req (:request request-msg)
        id-name (:id-name req)]
    {:request (:query-type req)
     id-name  (get-in req [:args id-name])}))

(defn print-pending-requests
  [chs-map]
  (println "*** :msg")
  (doseq [msg (dequeue-all-timeout (:msg chs-map) WAIT_MS)]
    (do
      (>!! (:failed chs-map) msg)
      (println msg)))
  (doseq [ch-name [:requests :next-pages :retries]]
    (println "***" ch-name)
    (doseq [msg (map request-msg->input-msg
                     (dequeue-all-timeout (get chs-map ch-name) WAIT_MS))]
      (do
        (>!! (:failed chs-map) (json/write-str msg))
        (json/pprint msg)))))

;;-----------------------

(def SLEEP_SECS 5)

(defn- wait-around []
  (println "\nSleeping for" SLEEP_SECS
           "seconds, to allow work to finish.")
  (loop [i 0]
    (if (< i SLEEP_SECS)
      (do
        (println "**")
        (flush)
        (<!! (timeout 1000))
        (recur (inc i))))))

;;-----------------------

(defn prep-shutdown
  [chs-map]
  (.addShutdownHook
   (Runtime/getRuntime)
   (Thread.
    (fn []
      ;; Stop any more input from coming in.
      (close! (:msg chs-map))
      (wait-around)
      (doseq [name [:failed :bodies :responses]]
        (print-msgs name (get chs-map name)))
      (print-pending-requests chs-map)))))
