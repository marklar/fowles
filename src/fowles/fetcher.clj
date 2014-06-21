(ns fowles.fetcher
  (:require [fowles
             [cfg :as cfg]
             [util :as util]
             [admitter :as admitter]
             [fetch-uris :as fetch-uris]
             [requester :as requester]
             [gatherer :as gatherer]
             [fetch-reporter :as fetch-reporter]]))

;; The name of the channel describes its contents.
;; Threads / Channels
;;   1. admitter : take video-ids from wherever (zmq socket?), put for requester.
;;   2. uris     : take video-ids, put URIs.
;;   3. gatherer : take URIs, async put responses.
;;   4. reporter : take responses, output wherever (zmq socket?)

;; Could also do this?
;; + response -> good-response | bad-response   (`split`)

(defn- fetch
  [api-key]
  (let [in->id            (admitter/admit-video-ids)
        id->uri           (fetch-uris/fetch-uris api-key in->id)
        uri->promise      (requester/mk-promises id->uri)
        promise->response (gatherer/gather-responses uri->promise)]
    (fetch-reporter/report promise->response))
  (while true))

(defn -main []
  (util/prep-shutdown)
  (let [api-key (cfg/get-api-key)]
    (if (nil? api-key)
      (println "Missing API key.")
      (fetch api-key))))

