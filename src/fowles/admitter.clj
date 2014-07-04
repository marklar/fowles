(ns fowles.admitter
  "Take in whatever input is required to perform data collection.
   + Source: from wherever (zmq socket?).
   + Data:   video-ids, queries.
   + Output: Put for requester."
  (:require [fowles.util :as util]
            [zeromq.zmq :as zmq]
            [clojure.core.async :refer [chan >!!]]))

(defn- mk-puller
  [ctx addr]
  (doto (zmq/socket ctx :pull)
    (zmq/connect addr)))

(defn- enq-from-puller
  [host port to-ch]
  (let [ctx  (zmq/context)
        addr (util/mk-connect-addr host port)]
    (with-open [puller (mk-puller ctx addr)]
      (println "Ready to receive input from addr:" addr)
      (while true
        (let [msg (zmq/receive-str puller)]
          (println "receiving:" msg)
          ;; -blocking- put
          (>!! to-ch msg))))))

;;-------------------

(defn from-puller
  ":: int -> chan"
  [host port]
  (let [to-ch (chan)]  ;; no buffer!
    (.start (Thread. #(enq-from-puller host port to-ch)))
    to-ch))
