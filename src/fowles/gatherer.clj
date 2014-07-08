(ns fowles.gatherer
  "Go-thread dedicated to outputing responses.
   Pull response off responses-channel.
   If response contains a nextPageToken (for search), queue up new URI."
  (:require [fowles.util :as util]
            [clojure.java.io :as io]
            [cemerick.url :refer [url-decode]]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [clojure.core.async :refer [go go-loop chan <! >!]]))

(def RETRIABLE_STATUS_CODES
  #{500 502 503 504})

(def RETRIABLE_ERRORS
  #{org.httpkit.ProtocolException
    java.io.IOException
    org.httpkit.client.TimeoutException})

(defn- retriable?
  ":: (Exception, int) -> bool"
  [error status]
  (or (contains? RETRIABLE_STATUS_CODES status)
      (contains? RETRIABLE_ERRORS (type error))))

(defn- success?
  ":: int -> bool"
  [status]
  (and (>= status 200)
       (< status 300)))

(defn- log [s msg error status]
  (println "** " s ":" msg
           "\n   error :" error
           "\n   status:" status))

(defn- handle-bad
  [msg error status retries-ch sleep-ch failed-ch]
  (if (retriable? error status)
    (do
      (log "requeueing" msg error status)
      (go (>! retries-ch msg))
      (go (>! sleep-ch :sleep)))
    (do
      (log "failed" msg error status)
      (go (>! failed-ch (json/write-str msg))))))

(defn- handle-good
  [msg body bodies-ch next-pages-ch]
  (let [resp-body  (json/read-str body)
        new-acc    (conj (:resp-bodies msg) resp-body)
        req        (:request msg)
        page-token (get resp-body "nextPageToken")]
    (if page-token
      (do
        (println "ok...")
        (go (>! next-pages-ch 
                {:request (assoc-in req [:args :pageToken] page-token)
                 :resp-bodies new-acc})))
      (do
        (println "ok.")
        (go (>! bodies-ch
                {:request req, :resp-bodies new-acc}))))))

;;--------------------------------

;; TODO: Make a hashmap of name->channel.
;; FIXME: rename chan.
(defn gather
  ":: (chans*) -> chan
   Given channel of responses, 'output' them."
  [responses-ch sleep-ch next-pages-ch retries-ch failed-ch]
  (let [bodies-ch (chan)]
    ;; Reading from responses-ch should never be nil.
    ;; Because of loop-backs, the requester can never know
    ;; when its input channels should close.
    (go-loop []
      (let [{:keys [status error body opts]} (<! responses-ch)
            msg                              (:msg opts)]
          (if (or error (not (success? status)))
            (handle-bad msg error status retries-ch sleep-ch failed-ch)
            (handle-good msg body bodies-ch next-pages-ch))
          (recur)))
    bodies-ch))
