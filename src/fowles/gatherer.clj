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

(defn- handle-bad-response
  ":: (hmap, chan, str) -> keyword"
  [{:keys [error status opts]} sleep-ch retries-ch failed-ch]
  (let [msg (:msg opts)]

    (if (retriable? error status)
      ;; re-queue the same requests
      (do
        (println "** requeueing:" msg)
        (println "   error :" error)
        (println "   status:" status)
        (println "")
        (>!! retries-ch msg)
        (>!! sleep-ch :sleep))

      ;; report failure
      (do
        (>!! failed-ch (json/write-str msg))
        ;; stdout
        (println "** failed:" msg)
        (println "   error :" error)
        (println "   status:" status)
        (println "")))))

(defn- handle-good-response
  ":: (hmap, chan, chan) -> keyword"
  [{:keys [body opts]} next-pages-ch bodies-ch]
  ;; FIXME: don't send clj-hmap, send original json.
  (let [resp-body (json/read-str body)
        msg       (:msg opts)]
    (println "ok")
    (let [new-acc    (conj (:resp-bodies msg) resp-body)
          page-token (get resp-body "nextPageToken")]
      (if page-token
        (let [new-msg 
              (assoc (assoc-in msg [:request :args :pageToken] page-token)
                :resp-bodies new-acc)]
          (>!! next-pages-ch new-msg))
        (>!! bodies-ch {:request (:request msg), :resp-bodies new-acc})))))

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
