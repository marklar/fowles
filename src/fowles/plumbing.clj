(ns fowles.plumbing
  (:require [clojure.core.async :refer [alts!! chan]]
            [fowles
             [requester :as requester]
             [gatherer :as gatherer]]))

;;
;; TODO: Add new channel:
;;   + failed-uris - between gatherer and for-later
;; 

(defn- dequeue
  ":: chan -> ()"
  [bodies-ch output-fn]
  (loop []
    (let [[body c] (alts!! [bodies-ch])]
      (if-not (nil? body)
        (do
          (output-fn body)
          (recur))))))

(defn- get-bodies-channel
  ":: (chan, str, int, int, int) -> chan"
  [uris-ch failed-file batch-size frequency-ms sleep-ms]
  (let [sleep-ch (chan)
        responses-ch
            (requester/get-responses uris-ch sleep-ch
                                     batch-size
                                     frequency-ms
                                     sleep-ms)
        bodies-ch (gatherer/gather responses-ch uris-ch
                                   sleep-ch failed-file)]
    bodies-ch))

;;
;; TODO: Change this behavior to match doc string.
;; We want the plumbing to PROVIDE the uris-ch,
;; not to require it from outside.
;;
(defn report
  ":: (chan, chan, int, int, int, str, fn) -> ()
   Given:
     + (for now, an input channel of URIs)
     + fetching behavior cfg (batch size, pause times)
     + file name for reporting failed fetches
     + function to call w/ body of each successful response
   Output:
     + failed URIs to `failed-file`
     + logging to stdout
   Return:
     + `uris-ch` for inputing URIs"
  [uris-ch
   batch-size frequency-ms sleep-ms
   failed-file output-fn]
  (let [;; uris-ch (chan 1000)
        bodies-ch (get-bodies-channel uris-ch
                                      failed-file
                                      batch-size
                                      frequency-ms
                                      sleep-ms)]
    (.start (Thread. #(dequeue bodies-ch output-fn)))))
  ;; uris-ch))

