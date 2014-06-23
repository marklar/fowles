(ns fowles.fetch.core
  "Fetch videos by id (or list of ids)."
  (:require [clojure.core.async :refer [chan]]
            [fowles
             [cfg :as cfg]
             [util :as util]
             [requester :as requester]]
            [fowles.fetch
             [admitter :as admitter]
             [uris :as uris]
             [reporter :as reporter]]))

;; TODO: Add new channels.
;;   + good-response-bodies - between gatherer and reporter
;;   + failed-uris - between gatherer and for-later
;; 
;; Take response, view it, and if:
;;   - good:
;;     + if there's a nextPageToken, put new URI onto uri-ch
;;     + put response-body onto channel for output-ing
;;   - bad:
;;     + if retriable, re-queue URI
;;     + if not, put URI onto _failed_ channel

(defn- fetch
  [api-key]
  (let [sleep-ch      (chan)
        ids-ch        (admitter/admit-video-ids)
        uris-ch       (uris/video-uris api-key ids-ch)
        responses-ch  (requester/get-responses uris-ch sleep-ch)]
    (reporter/report responses-ch uris-ch sleep-ch))
  (while true))

(defn -main []
  (util/prep-shutdown)
  (fetch (cfg/cfg-get :api-key)))
