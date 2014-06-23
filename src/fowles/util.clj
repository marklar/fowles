(ns fowles.util
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.core.async :refer [alts!!]]
            [clj-time
             [core :as t]
             [coerce :as c]
             ]))

;; Why doesn't this work?
(defn prep-shutdown []
  (let [start-time (t/now)]
    (.addShutdownHook
     (Runtime/getRuntime)
     (Thread. (fn []
                (let [elapsed (- (c/to-long t/now) (c/to-long start-time))]
                  (println "Shutting down...")
                  (println "elapsed time:" elapsed)))))))

;;------------------------

(defn- dequeue
  ":: chan -> ()"
  [bodies-ch output-fn]
  (loop []
    (let [[body c] (alts!! [bodies-ch])]
      (if-not (nil? body)
        (do
          (output-fn body)
          (recur))))))

(defn report
  ":: chan -> ()
   Given channel of responses, 'output' them in own Thread."
  [bodies-ch output-fn]
  (.start (Thread. #(dequeue bodies-ch output-fn))))

;;------------------------

;;
;; TODO: Rather than creating a new wrtr each time,
;; create one the first time and then reuse.
;;
;; Is one of these more efficient than the other?
;; (spit file-name
;;       (str (str/join "\n" strs) "\n")
;;       :append true))))
;;
(defn append-strs-to-file
  ":: ([str], str) -> ()"
  [strs file-name]
  (with-open [wrtr (io/writer file-name :append true)]
    (doseq [s strs] (.write wrtr (str s "\n")))))
