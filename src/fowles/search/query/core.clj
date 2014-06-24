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
             [plumbing :as plumbing]]
            [fowles.search.query
             [admitter :as admitter]
             [uris :as uris]
             [reporter :as reporter]]))

(defn- mk-uris-ch
  [api-key]
  (let [words-ch (admitter/admit-query-words)
        uris-ch  (uris/search-uris api-key words-ch)]
    uris-ch))

(defn- search
  [api-key]
  (plumbing/report (mk-uris-ch api-key)
                   reporter/output-video-and-channel-ids))

(defn -main []
  (search (cfg/cfg-get :api-key))
  (while true))
