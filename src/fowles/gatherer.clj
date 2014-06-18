(ns fowles.gatherer
  "Thread dedicated to gathering responses."
  (:require [clojure.core.async
             :refer [chan go split >! alts!!]
             :as async]))

(defn- foo
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
          (go (>! to-ch (deref v)))
          (recur))))))
  
;;-------------------------

(defn gather
  ":: chan -> chan
   Given channel of response-promises,
   return a channel of actual responses.
   Runs thread which takes promises and async puts responses."
  [from-ch]
  (let [to-ch (chan 50)]
    ;; We don't `map<' the channel because must make the `deref` async.
    (.start (Thread. #(foo from-ch to-ch)))
    to-ch))

