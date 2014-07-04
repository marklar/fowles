(ns fowles.plumbing
  (:require [clojure.core.async :refer [alts!! chan]]
            [fowles
             [requester :as requester]
             [gatherer :as gatherer]]))

;; TODO: rename 'body'.
(defn- dequeue
  ":: chan -> ()"
  [bodies-ch output-fn]
  ;; We do loop-recur instead of while
  ;; because maybe the channel will be closed.
  (loop []
    (let [[body c] (alts!! [bodies-ch])]
      (if (nil? body)
        (println "Plumbing thread exiting.")
        (do
          (output-fn body)
          (recur))))))

(defn- get-bodies-channel
  ":: ??"
  [requests-ch failed-ch api-keys
   batch-size interval-ms sleep-ms]
  (let [sleep-ch (chan)
        responses-ch (requester/mk-requests requests-ch api-keys
                                            sleep-ch
                                            batch-size
                                            interval-ms
                                            sleep-ms)
        ;; TODO: rename chan.
        bodies-ch (gatherer/gather responses-ch requests-ch
                                   sleep-ch failed-ch)]
    bodies-ch))

;;
;; TODO: Change this behavior to match doc string.
;; We want the plumbing to PROVIDE the requests-ch,
;; not to require it from outside.
;;
(defn report
  ":: ??
   Given:
     + (for now, an input channel of URIs)
     + fetching behavior cfg (batch size, pause times)
     + function to call w/ body of each successful response
   Output:
     + failed URIs to failed-ch
     + logging to stdout
   Return:
     + `requests-ch` for inputing requests"
  [requests-ch failed-ch api-keys
   batch-size interval-ms sleep-ms
   output-fn]
  ;; TODO: rename chan.
  (let [bodies-ch (get-bodies-channel requests-ch
                                      failed-ch
                                      api-keys
                                      batch-size
                                      interval-ms
                                      sleep-ms)]
    (.start (Thread. #(dequeue bodies-ch output-fn)))))
