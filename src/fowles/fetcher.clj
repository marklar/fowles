(ns fowles.fetcher
  (:require [fowles
             [cfg :as cfg]
             [util :as util]
             [admitter :as admitter]
             [requester :as requester]
             [gatherer :as gatherer]
             [reporter :as reporter]]))

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
  (->> (admitter/admit-video-ids)
       (requester/request-videos api-key)
       gatherer/gather-responses
       reporter/report-results)
  (while true))

(defn -main []
  (util/prep-shutdown)
  (let [api-key (cfg/get-api-key)]
    (if (nil? api-key)
      (println "Missing API key.")
      (fetch api-key))))

