(ns fowles.reporter
  "Thread dedicated to outputing responses."
  (:require [clojure.java.io :as io]
            [clojure.core.async :refer [go >! alts!!]]))

(defn- mk-page-uri
  ":: (str, str) -> str"
  [uri page-token]
  ;; If there's already a pageToken:
  ;;   + It'll be the last query-string arg.
  ;;   + Remove it.
  ;; Then append new pageToken.
  (let [[root-uri] (clojure.string/split uri #"\&pageToken=")]
    (str root-uri "&pageToken=" page-token)))

(defn- dequeue
  [from-ch output-fn]
  (loop []
    (let [[v c] (alts!! [from-ch])]
      (if (nil? v)
        nil
        (do
          (output-fn v)
          (recur))))))

;;-----------------------

;;
;; TODO: Rather than creating a new wrtr each time,
;; create one the first time and then reuse.
;;
(defn append-strs-to-file
  [strs file-name]
  ;; Is one of these more efficient than the other?
  ;; (spit file-name
  ;;       (str (clojure.string/join "\n" strs) "\n")
  ;;       :append true))))
  (with-open [wrtr (io/writer file-name :append true)]
    (doseq [s strs] (.write wrtr (str s "\n")))))

(defn queue-next-uri
  [uris-ch prev-uri resp-body]
  (if-let [page-token (get resp-body "nextPageToken")]
    (let [new-uri (mk-page-uri prev-uri page-token)]
      ;; (println "new-uri:" new-uri)
      (go (>! uris-ch new-uri)))))

(defn report
  ":: chan -> ()
   Given channel of responses, 'output' them in own Thread."
  [from-ch output-fn]
  (.start (Thread. #(dequeue from-ch output-fn))))

