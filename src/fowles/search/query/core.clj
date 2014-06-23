(ns fowles.search.query.core
  "Perform YouTube Data API 'search'.
   Only for:
     + type 'video'
     + matching query string
   From each search result, grab the videoId and channelId."
  (:require [clojure.core.async :refer [chan]]
            [fowles
             [cfg :as cfg]
             [util :as util]
             [requester :as requester]
             [gatherer :as gatherer]]
            [fowles.search.query
             [admitter :as admitter]
             [uris :as uris]
             [reporter :as reporter]]))

(defn- search
  [api-key]
  (let [sleep-ch      (chan)
        words-ch      (admitter/admit-query-words)
        uris-ch       (uris/search-uris api-key words-ch)
        responses-ch  (requester/get-responses uris-ch sleep-ch)
        bodies-ch     (gatherer/gather responses-ch uris-ch sleep-ch)]
    (util/report bodies-ch reporter/output-video-and-channel-ids))
  (while true))

(defn -main []
  (search (cfg/cfg-get :api-key)))
