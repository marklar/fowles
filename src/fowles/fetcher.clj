(ns fowles.fetcher
  ;; [com.keminglabs.zmq-async.core :refer [register-socket!]]
  (:require [fowles
             [cfg :as cfg]
             [admitter :as admitter]
             [requester :as requester]
             [gatherer :as gatherer]
             [reporter :as reporter]]
            [clj-time.core :as t]))

;; The name of the channel describes its contents.
;; Threads / Channels
;;   1. admitter  : take video-ids from wherever (zmq socket?), put for requester.
;;   2. requester : take video-ids, put response-promises.  [Not Thread]
;;   3. gatherer  : take response-promises, async put responses
;;   4. reporter  : take responses, output wherever (zmq socket?)

;; Could also do this?
;; + response -> good-response | bad-response   (`split`)

(defn- fetch
  [api-key]
  (->> (admitter/admit)
       (requester/request api-key)
       gatherer/gather
       reporter/report)
  (while true))

;; Why doesn't this work?
(defn- prep-shutdown []
  (let [start-time (t/now)]
    (.addShutdownHook
     (Runtime/getRuntime)
     (Thread. (fn []
                (let [elapsed (- t/now start-time)]
                  (println "Shutting down...")
                  (println "elapsed time:" elapsed)))))))

(defn -main []
  (prep-shutdown)
  (let [api-key (cfg/get-api-key)]
    (if (nil? api-key)
      (println "Missing API key.")
      (fetch api-key))))

