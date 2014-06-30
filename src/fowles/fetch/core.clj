(ns fowles.fetch.core
  "Fetch videos by id (or list of ids)."
  (:refer-clojure :exclude [merge])
  (:require [clojure.core.async :refer [merge] :as async]
            [fowles
             [plumbing :as plumbing]
             [failed :as failed]
             [cfg :as secret-cfg]]
            [fowles.fetch
             [cfg :as cfg]
             [admitter :as admitter]
             [requests :as requests]
             [reporter :as reporter]]))

(defn- mk-vid-and-chan-ids-chs
  "returns 2 channels: vid-ids-ch & chan-ids-ch!"
  []
  (admitter/id-chs-from-puller
   (cfg/in-host)
   (cfg/in-port)
   (cfg/num-per-request :videos)
   (cfg/num-per-request :channels)))

(defn- mk-typed-requests-ch [q-type ids-ch]
  (requests/get-requests-ch q-type ids-ch
                            (cfg/part q-type) (cfg/fields q-type)))

(defn- mk-requests-ch []
  (let [[v-ch c-ch] (mk-vid-and-chan-ids-chs)]
    (async/merge
     [(mk-typed-requests-ch :videos   v-ch)
      (mk-typed-requests-ch :channels c-ch)])))

(defn- get-output-fn []
  (reporter/mk-videos-pusher (cfg/out-host) (cfg/out-port)))

(defn- mk-failed-ch []
  (failed/mk-ch (cfg/failed-host) (cfg/failed-port)))

(defn- fetch []
  ;; Start plumbing Thread.
  (plumbing/report (mk-requests-ch)
                   (mk-failed-ch)
                   (secret-cfg/api-keys)
                   (cfg/batch-size)
                   (cfg/interval-ms)
                   (cfg/sleep-ms)
                   (get-output-fn))
  (while true))

;;---------------

(defn -main []
  (cfg/validate)
  (fetch))
