(ns fowles.reporter
  "Thread dedicated to outputing responses.
   Pull response off responses-channel.
   In the case of search, if response contains a nextPageToken, queue up new URI."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [clojure.core.async :refer [go >!! >! alts!!]]))

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

(defn- mk-page-uri
  ":: (str, str) -> str"
  [uri page-token]
  ;; If there's already a pageToken:
  ;;   + It'll be the last query-string arg.
  ;;   + Remove it.
  ;; Then append new pageToken.
  (let [[root-uri] (str/split uri #"\&pageToken=")]
    (str root-uri "&pageToken=" page-token)))

(defn- handle-bad-response
  ":: (hmap, chan) -> keyword"
  [{:keys [error status opts]} uris-ch]
  (let [uri (:url opts)]
    (if (retriable? error status)
      ;; re-queue the same URI
      ;; TODO: add a "&retryNum=<num>" to end of URL.
      ;; That way we can track which ones get tried (and how often).
      (do
        (println "** retrying:" uri)
        (go (>!! uris-ch uri))
        :sleep)
      ;; report failure
      (do
        (println "** failed:" uri)
        (println "   error :" error)
        (println "   status:" status)
        :no-sleep))))

(defn- handle-good-response
  ":: (hmap, fn, chan) -> keyword"
  [{:keys [body opts]} output-fn uris-ch]
  (let [resp-body (json/read-str body)
        uri (:url opts)]
    (println "ok")
    ;; use it
    (output-fn resp-body)
    ;; queue up nextPage, if any
    (if-let [page-token (get resp-body "nextPageToken")]
      (>!! uris-ch
           (mk-page-uri uri page-token)))
    :no-sleep))

(defn- handle-response
  ":: (hmap, fn, chan) -> keyword
   Return either :sleep or :no-sleep, to indicate what to do."
  [response output-fn uris-ch]
  (let [{:keys [status error]} response]
    (if (or error (not (success? status)))
      (handle-bad-response response uris-ch)
      (handle-good-response response output-fn uris-ch))))

;; (defn- get-sleep-secs
;;   ":: (keyword, int) -> int"
;;   [directive prev-secs]
;;   (if (= directive :no-sleep)
;;     0
;;     (* prev-secs 2)))

(defn- dequeue
  [responses-ch uris-ch sleep-ch output-fn]
  (loop [sleep? :no-sleep]
    ;; Possibly tell the requester thread to chill out for a sec.
    (if (= sleep? :sleep)
      (go (>! sleep-ch :sleep)))
    ;; Grab another response.
    (let [[response c] (alts!! [responses-ch])]
      (if (nil? response)
        nil
        (let [new-sleep? (handle-response response output-fn uris-ch)]
          (recur new-sleep?))))))

;;--------------------------------

;;
;; TODO: Rather than creating a new wrtr each time,
;; create one the first time and then reuse.
;;
;; Is one of these more efficient than the other?
;; (spit file-name
;;       (str (str/join "\n" strs) "\n")
;;       :append true))))
;;
(defn append-strs-to-file
  ":: ([str], str) -> ()"
  [strs file-name]
  (with-open [wrtr (io/writer file-name :append true)]
    (doseq [s strs] (.write wrtr (str s "\n")))))

(defn report
  ":: chan -> ()
   Given channel of responses, 'output' them in own Thread."
  [responses-ch uris-ch sleep-ch output-fn]
  (.start (Thread. #(dequeue responses-ch uris-ch sleep-ch output-fn))))
