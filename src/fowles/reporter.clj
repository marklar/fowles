(ns fowles.reporter
  "Thread dedicated to outputing responses."
  (:require [clojure.core.async
             ;; split
             :refer [chan alts!!]]))

(defn- output
  "For now, 'output' merely means write to stdout.
   But eventually, we may want to insert into LevelDB
   or perhaps enqueue on a 0MQ socket."
  [response-json]
  (println response-json))

(defn- dequeue
  [from-ch]
  (loop []
    (let [[v c] (alts!! [from-ch])]
      (if (nil? v)
        nil
        (do
          (output v)
          (recur))))))

;;------------

(defn report-results
  ":: chan -> ()
   Given channel of responses, 'output' them in own Thread."
  [from-ch]
  (.start (Thread. #(dequeue from-ch))))
