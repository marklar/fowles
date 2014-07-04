(ns fowles.requester
  (:require [org.httpkit.client :as http]
            [clojure.core.async :refer [chan <!! >!! alt!!]]
            [fowles.util :as util]
            [fowles.uris :as uris]))

(defn- async-get
  [req api-key result-ch]
  (let [new-req (assoc-in req [:args :key] api-key)
        uri     (uris/mk-uri new-req)]
    (http/get uri
              {:request new-req}  ;; gets added to 'opts' in callback
              #(>!! result-ch %))))

(defn- deq-and-req
  [ch]
  (let [v (<!! ch)]
    (>!! ch v)
    v))

(defn- sleep-or-get
  [requests-ch api-keys to-ch sleep-ch
   batch-size interval-ms sleep-ms]

  (let [keys-ch (chan (count api-keys))]
    (doseq [k api-keys] (>!! keys-ch k))

    (loop [i 0]
      
      (if (= i batch-size)
        ;; We always pause after every batch.
        (do
          (Thread/sleep interval-ms)
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
         
         ;; Grab a request and do an async-get.
         ;; Do NOT close.  If you see a nil, it's because the
         ;; orig input channel closed, but *not* the retries.
         requests-ch ([req] (if (nil? req)

                              ;; We're done!
                              ;; But don't close to-ch.
                              ;; Can safely do that only when
                              ;; retries-ch closes.
                              ;; But how do we know when to do that?
                              nil
                              
                              (do
                                (let [api-key (deq-and-req keys-ch)]
                                  (async-get req api-key to-ch)
                                  (recur (inc i))))))
         
         :priority true)))))

;;------------------------

(defn mk-requests
  ":: ?? "
  [requests-ch api-keys sleep-ch batch-size interval-ms sleep-ms]
  (let [to-ch (chan)]
    (.start (Thread. #(sleep-or-get requests-ch api-keys to-ch
                                    sleep-ch
                                    batch-size
                                    interval-ms
                                    sleep-ms)))
    to-ch))
