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
  [uris-ch]
  (let [sleep-ch     (chan)
        responses-ch (requester/get-responses uris-ch sleep-ch)
        bodies-ch    (gatherer/gather responses-ch uris-ch sleep-ch)]
    bodies-ch))

(defn report
  ":: chan -> ()
   Given channel of responses, 'output' them in own Thread."
  [uris-ch output-fn]
  (println "in plumbing/report")
  (let [bodies-ch (get-bodies-channel uris-ch)]
    (.start (Thread. #(dequeue bodies-ch output-fn)))))

