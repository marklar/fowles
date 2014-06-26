(ns fowles.failed
  (:require [clojure.core.async :refer [<!! chan]]
            [zeromq.zmq :as zmq]
            [fowles.util :as util]))

(defn- deq-failed
  [failed-ch host port]
  (let [pusher (util/mk-pusher host port)]
    (while true
      (zmq/send-str pusher (<!! failed-ch)))))

;;--------------------

(defn mk-ch
  [host port]
  (let [failed-ch (chan)]
    (.start (Thread. #(deq-failed failed-ch host port)))
    failed-ch))
