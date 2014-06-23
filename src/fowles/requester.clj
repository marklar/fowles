(ns fowles.requester
  (:require [org.httpkit.client :as http]
            [clojure.core.async :refer [chan go >! alt!!]]))

(defn- async-get
  [uri result-ch]
  (http/get uri #(go (>! result-ch %))))

(defn- sleep-or-get
  [from-ch to-ch sleep-ch]
  (loop [i 0]

    (if (= i 5)
      ;; We always sleep after every 5 requests.
      (do
        (Thread/sleep 500)
        (recur 0))

      ;; Perhaps sleep, perhaps request.
      (alt!!
       ;; The gatherer tells you to sleep.
       ;; Oblige only if didn't just do it.
       sleep-ch ([_] (if (> i 0)
                       (do
                         (println " ---- SLEEPING ----")
                         (Thread/sleep 1000)))
                   (recur 0))
       
       ;; Grab a URI and do an async-get.  (Don't close.)
       from-ch  ([uri] (if (nil? uri)
                         (recur i)
                         (do
                           (async-get uri to-ch)
                           (recur (inc i)))))
       
       :priority true))))

;;------------------------

(defn get-responses
  ":: chan -> chan"
  [from-ch sleep-ch]
  (let [to-ch (chan)]
    (.start (Thread. #(sleep-or-get from-ch to-ch sleep-ch)))
    to-ch))
