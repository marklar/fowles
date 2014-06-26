(ns fowles.gatherer
  "Thread dedicated to outputing responses.
   Pull response off responses-channel.
   In the case of search, if response contains a nextPageToken, queue up new URI."
  (:require [clojure.java.io :as io]
            [cemerick.url :refer [url-decode]]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [clojure.core.async :refer [chan go >!! >! alts!!]]))

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

(defn- get-first-id-from-fetch-uri
  [uri]
  (let [[before after]  (str/split uri #"\&id=")
        [first-id rest] (str/split after #",")]
    first-id))

(defn- replace-api-key
  [uri]
  uri)

(defn- handle-bad-response
  ":: (hmap, chan, str) -> keyword"
  [{:keys [error status opts]} uris-ch
   ;; failed-file
   ]
  (let [uri (url-decode (:url opts))]

    ;; (if (= 403 status)
    ;;
    ;; If the status code is 403,
    ;; that means we've reached our daily usage limits.
    ;; Switch to a new API key.
    ;;

    (if (retriable? error status)
      ;; re-queue the same URI
      (do
        (println "** requeueing:" uri)
        (println "   error :" error)
        (println "   status:" status)
        (println "")
        (go (>! uris-ch uri))
        :sleep)
      ;; report failure
      (do
        ;; write failed uri to file
        ;; (spit failed-file (str uri "\n") :append true)
        (spit "io/fetch/failed.txt" (str uri "\n") :append true)
        ;; stdout
        (println "** failed:" uri)
        (println "   error :" error)
        (println "   status:" status)
        (println "")
        :no-sleep))))

(defn- handle-good-response
  ":: (hmap, chan, chan) -> keyword"
  [{:keys [body opts]} uris-ch bodies-ch]
  (let [resp-body (json/read-str body)
        uri (url-decode (:url opts))]
    (println "ok")
    ;; use it
    (>!! bodies-ch resp-body)
    ;; queue up nextPage, if any
    (if-let [page-token (get resp-body "nextPageToken")]
      (>!! uris-ch
           (mk-page-uri uri page-token)))
    :no-sleep))

(defn- handle-response
  ":: (hmap, chan, chan, str) -> keyword
   Return either :sleep or :no-sleep, to indicate what to do."
  [response uris-ch bodies-ch
   ;; failed-file
   ]
  (let [{:keys [status error]} response]
    (if (or error (not (success? status)))
      (handle-bad-response response uris-ch
                           ;; failed-file
                           )
      (handle-good-response response uris-ch bodies-ch))))

(defn- dequeue
  [responses-ch uris-ch sleep-ch bodies-ch
   ;; failed-file
   ]
  (loop [sleep? :no-sleep]
    ;; Possibly tell the requester thread to chill out for a sec.
    (if (= sleep? :sleep)
      (go (>! sleep-ch :sleep)))
    ;; Grab another response.
    (let [[response c] (alts!! [responses-ch])]
      (if (nil? response)
        nil
        (let [new-sleep? (handle-response response uris-ch bodies-ch
                                          ;; failed-file
                                          )]
          (recur new-sleep?))))))

;;--------------------------------

(defn gather
  ":: (chan, chan, chan) -> chan
   Given channel of responses, 'output' them in own Thread."
  [responses-ch uris-ch sleep-ch
   ;; failed-file
   ]
  (let [bodies-ch (chan)]
    (.start (Thread. #(dequeue responses-ch uris-ch sleep-ch bodies-ch
                               ;; failed-file
                               )))
    bodies-ch))
