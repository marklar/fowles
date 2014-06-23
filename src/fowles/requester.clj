(ns fowles.requester
  (:require [org.httpkit.client :as http]
            [clojure.core.async :refer [chan go >! alt!!]]))

(defn- async-get
  [uri result-ch]
  (http/get uri #(go (>! result-ch %))))

(defn- sleep-or-get
  [from-ch to-ch sleep-ch]
  (loop []
    (alt!!
     ;; If the reporter tells you to sleep, sleep for 2 secs.
     sleep-ch ([v] (do
                     (println " ---- SLEEPING ----")
                     (Thread/sleep 2000)))
     ;; Grab a URI and do an async-get.
     from-ch  ([uri]
                 ;; Cannot `close!` here.  Might happen *before*
                 ;; the other go-threads have a chance to `>!`.
                 (if-not (nil? uri)
                   (async-get uri to-ch)))
     :priority true)
    (recur)))

;;------------------------

(defn get-responses
  ":: chan -> chan"
  [from-ch sleep-ch]
  (let [to-ch (chan 1000)]
    (.start (Thread. #(sleep-or-get from-ch to-ch sleep-ch)))
    to-ch))
