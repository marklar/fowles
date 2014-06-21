(ns fowles.gatherer
  "Thread dedicated to gathering responses."
  (:require [org.httpkit.client :as http]
            [clojure.core.async
             :refer [chan go >! alts!!]
             :as async]))

(def BUFFER_SIZE 100000)

(defn- requeue-vals
  [from-ch to-ch]
  (loop []
    ;; `alts!!` blocks until completed.
    (let [[v c] (alts!! [from-ch])]
      (if (nil? v)
        ;; Cannot `close!` here.  Might happen *before*
        ;; the other go-threads have a chance to `>!`.
        ;; (close! to-ch)
        nil
        (do
          (go (>! to-ch @v))
          (recur))))))
  
;;-------------------------

(defn gather-responses
  ":: chan -> chan
   Given channel of URIs,
   first map that to a channel of response-promises,
   then return a channel of actual responses.
   Runs thread which takes promises and async puts responses."
  [from-ch]
  (let [to-ch (chan BUFFER_SIZE)]
    ;; We don't `map<' the channel because must make the `deref` async.
    (.start (Thread. #(requeue-vals from-ch to-ch)))
    to-ch))
