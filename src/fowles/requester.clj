(ns fowles.requester
  (:require [org.httpkit.client :as http]
            [clojure.core.async :refer [chan <!! >!! alt!!]]
            [fowles.util :as util]
            [fowles.uris :as uris]))

;; msg: {:request {}, :acc []}
(defn- async-get
  [msg api-key result-ch]
  (let [new-msg (assoc-in msg [:request :args :key] api-key)
        uri     (uris/mk-uri (:request new-msg))]
    (http/get uri
              {:msg new-msg}  ;; gets added to 'opts' in callback
              #(>!! result-ch %))))

;; TODO: Put this in util.
(defn- deq-and-req
  [ch]
  (let [v (<!! ch)]
    (>!! ch v)
    v))

(defn- sleep-or-get
  [requests-ch api-keys to-ch sleep-ch next-pages-ch retries-ch
   batch-size interval-ms sleep-ms]

  (let [keys-ch (chan (count api-keys))]
    (doseq [k api-keys] (>!! keys-ch k))

    (loop [i 0]
      
      (if (= i batch-size)
        ;; We always pause after every batch.
        (do
          (System/gc)
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

         ;; TODO: DRY this up.  Use alts!! ?

         ;; Do follow-on pages first.
         next-pages-ch ([msg] (if (nil? msg)
                                nil
                                (let [api-key (deq-and-req keys-ch)]
                                  (async-get msg api-key to-ch)
                                  (println "+ next")
                                  (recur (inc i)))))

         ;; Next prioritize retries???  (Get rid of internal state.)
         ;; Or maybe we want to wait on these; YT is having issues.
         retries-ch    ([msg] (if (nil? msg)
                                nil
                                (let [api-key (deq-and-req keys-ch)]
                                  (async-get msg api-key to-ch)
                                  (println "- retry")
                                  (recur (inc i)))))

         ;; Finally, grab a brand-new request...
         requests-ch   ([msg] (if (nil? msg)
                                nil
                                (let [api-key (deq-and-req keys-ch)]
                                  (async-get msg api-key to-ch)
                                  (println "% regular")
                                  (recur (inc i)))))
         
         :priority true)))))

;;------------------------

(defn mk-requests
  ":: ?? "
  [requests-ch api-keys sleep-ch next-pages-ch retries-ch
   batch-size interval-ms sleep-ms]
  (let [to-ch (chan)]  ;; responses-ch
    (.start (Thread. #(sleep-or-get requests-ch api-keys to-ch
                                    sleep-ch next-pages-ch retries-ch
                                    batch-size
                                    interval-ms
                                    sleep-ms)))
    to-ch))
