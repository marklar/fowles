(ns fowles.failed
  (:require [clojure.core.async :refer [<!! chan]]
            [zeromq.zmq :as zmq]
            [fowles.util :as util]))

(defn- deq-failed
  [failed-ch host port]
  (let [pusher (util/mk-pusher host port)]
    (loop []
      (if-let [v (<!! failed-ch)]
        (do
          (zmq/send-str pusher v)
          (recur))
        (println "Failed pusher exiting.")))))

;;--------------------

(defn mk-ch
  [host port]
  (let [failed-ch (chan)]
    (.start (Thread. #(deq-failed failed-ch host port)))
    failed-ch))
