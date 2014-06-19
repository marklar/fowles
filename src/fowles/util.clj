(ns fowles.util
  (:require [clj-time.core :as t]))

;; Why doesn't this work?
(defn prep-shutdown []
  (let [start-time (t/now)]
    (.addShutdownHook
     (Runtime/getRuntime)
     (Thread. (fn []
                (let [elapsed (- t/now start-time)]
                  (println "Shutting down...")
                  (println "elapsed time:" elapsed)))))))
