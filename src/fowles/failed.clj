(ns fowles.failed
  (:require [clojure.core.async :refer [go-loop <! chan close!]]
            [zeromq.zmq :as zmq]
            [fowles.util :as util]))

(defn mk-ch
  [host port]
  (let [failed-ch (chan)
        pusher    (util/mk-pusher host port)]
    (go-loop []
      (if-let [v (<! failed-ch)]
      (do
        (zmq/send-str pusher v)
        (recur))
      (do
        (println "Failed pusher exiting.")
        (zmq/close pusher)
        (close! failed-ch))))
    failed-ch))
