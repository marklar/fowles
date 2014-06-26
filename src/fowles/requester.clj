(ns fowles.requester
  (:require [org.httpkit.client :as http]
            [clojure.core.async :refer [chan go <!! >! >!! alt!!]]
            [fowles.util :as util]
            [fowles.uris :as uris]))

(defn- async-get
  [{:keys [query-type args] :as req}
   api-key result-ch]
  (let [new-req (util/update-request-arg req :key api-key)
        uri     (uris/mk-uri new-req)]
    (http/get uri
              {:request new-req}  ;; gets added to 'opts' in callback
              #(go (>! result-ch %)))))

(defn- deq-and-req
  [ch]
  (let [v (<!! ch)]
    (>!! ch v)
    v))

(defn- sleep-or-get
  [from-ch api-keys to-ch sleep-ch
   batch-size frequency-ms sleep-ms]

  (let [keys-ch (chan (count api-keys))]
    (doseq [k api-keys] (>!! keys-ch k))

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
         from-ch  ([req] (if (nil? req)
                           (recur i)
                           (do
                             (let [api-key (deq-and-req keys-ch)]
                               (async-get req api-key to-ch)
                               (recur (inc i))))))
         
         :priority true)))))

;;------------------------

(defn mk-requests
  ":: ?? "
  [from-ch api-keys sleep-ch batch-size frequency-ms sleep-ms]
  (let [to-ch (chan)]
    (.start (Thread. #(sleep-or-get from-ch api-keys to-ch
                                    sleep-ch
                                    batch-size
                                    frequency-ms
                                    sleep-ms)))
    to-ch))
