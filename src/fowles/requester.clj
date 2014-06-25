(ns fowles.requester
  (:require [org.httpkit.client :as http]
            [clojure.core.async :refer [chan go >! alt!!]]))

(defn- async-get
  [uri result-ch]
  (http/get uri #(go (>! result-ch %))))

(defn- sleep-or-get
  [from-ch to-ch sleep-ch
   batch-size frequency-ms sleep-ms]
  (loop [i 0]

    (if (= i batch-size)
      ;; We always pause after every batch.
      (do
        (Thread/sleep frequency-ms)
        (recur 0))

      ;; Perhaps sleep, perhaps request.
      (alt!!
       ;; The gatherer tells you to sleep.
       ;; Oblige only if didn't just do it.
       sleep-ch ([_] (if (> i 0)
                       (do 
                         (println " ---- SLEEPING ----")
                         (Thread/sleep sleep-ms)))
                   (recur 0))
       
       ;; Grab a URI and do an async-get.
       ;; Do NOT close.  If you see a nil, it's because the
       ;; orig input channel closed, but *not* the retries.
       from-ch  ([uri] (if (nil? uri)
                         (recur i)
                         (do
                           (println "getting URI:" uri)
                           (async-get uri to-ch)
                           (recur (inc i)))))
       
       :priority true))))

;;------------------------

(defn get-responses
  ":: chan -> chan"
  [from-ch sleep-ch batch-size frequency-ms sleep-ms]
  (let [to-ch (chan)]
    (.start (Thread. #(sleep-or-get from-ch to-ch
                                    sleep-ch
                                    batch-size
                                    frequency-ms
                                    sleep-ms)))
    to-ch))
