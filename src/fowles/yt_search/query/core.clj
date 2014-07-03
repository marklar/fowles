(ns fowles.search.query.core
  "Perform YouTube Data API 'search'.
   Only for:
     + type 'video'
     + matching query string
   From each search result, grab the videoId and channelId."
  (:require [clojure.core.async :refer [chan]]
            [fowles
             [plumbing :as plumbing]]
            [fowles.search
             [reporter :as reporter]]
            [fowles.search.query
             [cfg :as cfg]
             [admitter :as admitter]
             [uris :as uris]]))

(defn- mk-uris-ch []
  (let [words-ch (admitter/admit-query-words-from-file (cfg/in-file)
                                                       (cfg/start-date)
                                                       (cfg/end-date))
        uris-ch  (uris/search-uris (cfg/api-key)
                                   (cfg/part)
                                   (cfg/fields)
                                   words-ch)]
    uris-ch))

(defn- search []
  (plumbing/report (mk-uris-ch)
                   (cfg/batch-size)
                   (cfg/frequency-ms)
                   (cfg/sleep-ms)
                   (cfg/failed-file)
                   (partial reporter/output-video-and-channel-ids
                            (cfg/out-file))))

(defn -main []
  (cfg/validate)
  (search)
  (while true))
