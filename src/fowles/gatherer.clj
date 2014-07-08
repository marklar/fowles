(ns fowles.gatherer
  "Thread dedicated to outputing responses.
   Pull response off responses-channel.
   If response contains a nextPageToken (for search), queue up new URI."
  (:require [fowles.util :as util]
            [clojure.java.io :as io]
            [cemerick.url :refer [url-decode]]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [clojure.core.async :refer [chan >!! alts!!]]))

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

(defn- handle-bad-response
  ":: (hmap, chan, str) -> keyword"
  [{:keys [error status opts]} sleep-ch retries-ch failed-ch]
  (let [msg (:msg opts)]

    (if (retriable? error status)
      ;; re-queue the same requests
      (do
        (log "requeueing" msg error status)
        (>!! retries-ch msg)
        (>!! sleep-ch :sleep))
      ;; report failure
      (do
        (log "failed" msg error status)
        (>!! failed-ch (json/write-str msg))))))

(defn- handle-good-response
  ":: (hmap, chan, chan) -> keyword"
  [{:keys [body opts]} next-pages-ch bodies-ch]
  ;; FIXME: don't send clj-hmap, send original json.
  ;; However, we do need to extract the nextPageToken.
  (let [resp-body  (json/read-str body)
        msg        (:msg opts)
        new-acc    (conj (:resp-bodies msg) resp-body)
        req        (:request msg)
        page-token (get resp-body "nextPageToken")]
      (println "ok")
      (if page-token
        ;; We have another page to grab.  Loop back.
        (>!! next-pages-ch 
             {:request (assoc-in req [:args :pageToken] page-token)
              :resp-bodies new-acc})
        ;; We're done.  Send accumulated result to reporter.
        (>!! bodies-ch
             {:request req
              :resp-bodies new-acc}))))

(defn- handle-response
  ":: (hmap, chan, chan, str) -> keyword"
  [response sleep-ch next-pages-ch retries-ch bodies-ch failed-ch]
  (let [{:keys [status error]} response]
    (if (or error (not (success? status)))
      (handle-bad-response response sleep-ch retries-ch failed-ch)
      (handle-good-response response next-pages-ch bodies-ch))))

(defn- dequeue
  [responses-ch sleep-ch next-pages-ch retries-ch
   bodies-ch failed-ch]
  (loop []
    (let [[response c] (alts!! [responses-ch])]
      (if (nil? response)
        nil
        (do
          (handle-response response sleep-ch
                           next-pages-ch retries-ch
                           bodies-ch failed-ch)
          (recur))))))

;;--------------------------------

;; TODO: Make a hashmap of name->channel.

(defn gather
  ":: (chans*) -> chan
   Given channel of responses, 'output' them in own Thread."
  [responses-ch sleep-ch next-pages-ch retries-ch failed-ch]
  ;; FIXME: rename chan.
  (let [bodies-ch (chan)]
    (.start (Thread. #(dequeue responses-ch
                               sleep-ch next-pages-ch retries-ch
                               bodies-ch failed-ch)))
    bodies-ch))
