(ns fowles.admitter
  "Take in whatever input is required to perform data collection.
   + Source: from wherever (zmq socket?).
   + Data:   video-ids, queries.
   + Output: Put for requester."
  (:require [fowles.util :as util]
            [zeromq.zmq :as zmq]
            [clojure.core.async :refer [go chan >!]]))

(defn- mk-puller
  [ctx addr]
  (doto (zmq/socket ctx :pull)
    (zmq/connect addr)))

(defn- report-unused-input
  [msg]
  ;; TODO: Report to failure sink!
  )

;;-------------------

(defn from-puller
  ":: int -> chan"
  [host port]
  (let [to-ch  (chan)
        ctx    (zmq/context)
        addr   (util/mk-connect-addr host port)]
    (go
     (with-open [puller (mk-puller ctx addr)]
       (println "Ready to receive input from addr:" addr)
       (loop []
         (let [msg (zmq/receive-str puller)]
           (println "receiving:" msg)
           (if (>! to-ch msg)
             (recur)
             (report-unused-input msg))))))
    to-ch))
