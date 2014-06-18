(ns fowles.admitter
  ;; [com.keminglabs.zmq-async.core :refer [register-socket!]]
  (:require [clojure.core.async :refer [chan go >!! close!]]))

(def VIDEO_IDS
  ["7lCDEYXw3mM" "MjtOzLfebgY" "6QIw1BQIvT4" "2xJWQPdG7jE"])

(def all-video-ids
  (take 100 (cycle VIDEO_IDS)))

(defn- enqueue
  [to-ch]
  (doseq [id all-video-ids]
    (>!! to-ch id))
  (close! to-ch))

;;----------------------

(defn admit []
  (let [to-ch (chan 1000)]
    (.start (Thread. (enqueue to-ch)))
    to-ch))
