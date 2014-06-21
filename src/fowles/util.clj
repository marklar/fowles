(ns fowles.util
  (:require [clj-time
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
