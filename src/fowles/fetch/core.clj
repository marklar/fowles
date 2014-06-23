(ns fowles.fetch.core
  "Fetch videos by id (or list of ids)."
  (:require [clojure.core.async :refer [chan]]
            [fowles
             [cfg :as cfg]
             [util :as util]
             [requester :as requester]
             [gatherer :as gatherer]]
            [fowles.fetch
             [admitter :as admitter]
             [uris :as uris]
             [reporter :as reporter]]))

;;
;; TODO: Add new channel:
;;   + failed-uris - between gatherer and for-later
;; 

(defn- fetch
  [api-key]
  (let [sleep-ch      (chan)
        ids-ch        (admitter/admit-video-ids)
        uris-ch       (uris/video-uris api-key ids-ch)
        responses-ch  (requester/get-responses uris-ch sleep-ch)
        bodies-ch     (gatherer/gather responses-ch uris-ch sleep-ch)]
    (util/report bodies-ch reporter/output-videos))
  (while true))

(defn -main []
  (util/prep-shutdown)
  (fetch (cfg/cfg-get :api-key)))
