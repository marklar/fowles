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
  [{:keys [error status opts]} requests-ch failed-ch]
  (let [request (:request opts)]

    ;; (if (= 403 status)
    ;;
    ;; If the status code is 403,
    ;; that means we've reached our daily usage limits.
    ;; Switch to a new API key.
    ;;

    (if (retriable? error status)
      ;; re-queue the same requests
      (do
        (println "** requeueing:" request)
        (println "   error :" error)
        (println "   status:" status)
        (println "")
        (>!! requests-ch request)
        :sleep)
      ;; report failure
      (do
        (>!! failed-ch (json/write-str request))
        ;; stdout
        (println "** failed:" request)
        (println "   error :" error)
        (println "   status:" status)
        (println "")
        :no-sleep))))

(defn- maybe-add-next-page
  [request resp-body requests-ch]
  (if-let [page-token (get resp-body "nextPageToken")]
    (let [new-request (assoc-in request [:args :pageToken] page-token)]
      (>!! requests-ch new-request))))

(defn- handle-good-response
  ":: (hmap, chan, chan) -> keyword"
  [{:keys [body opts]} requests-ch bodies-ch]
  (let [resp-body (json/read-str body)
        req       (:request opts)]
    (println "ok")
    (>!! bodies-ch resp-body)
    (maybe-add-next-page req resp-body requests-ch))
  :no-sleep)

(defn- handle-response
  ":: (hmap, chan, chan, str) -> keyword
   Return either :sleep or :no-sleep, to indicate what to do."
  [response requests-ch bodies-ch failed-ch]
  (let [{:keys [status error]} response]
    (if (or error (not (success? status)))
      (handle-bad-response response requests-ch failed-ch)
      (handle-good-response response requests-ch bodies-ch))))

(defn- dequeue
  [responses-ch requests-ch sleep-ch bodies-ch failed-ch]
  (loop [sleep? :no-sleep]
    ;; Possibly tell the requester thread to chill out for a sec.
    (if (= sleep? :sleep)
      (>!! sleep-ch :sleep))
    ;; Grab another response.
    (let [[response c] (alts!! [responses-ch])]
      (if (nil? response)
        nil
        (let [new-sleep? (handle-response response requests-ch
                                          bodies-ch failed-ch)]
          (recur new-sleep?))))))

;;--------------------------------

(defn gather
  ":: (chan, chan, chan, chan) -> chan
   Given channel of responses, 'output' them in own Thread."
  [responses-ch requests-ch sleep-ch failed-ch]
  (let [bodies-ch (chan)]
    (.start (Thread. #(dequeue responses-ch requests-ch
                               sleep-ch bodies-ch failed-ch)))
    bodies-ch))
