(ns fowles.search.query.admitter
  "Take in whatever input is required to perform data collection.
   + Source: from wherever (zmq socket?).
   + Data:   video-ids, queries.
   + Output: Put for requester."
  ;; [com.keminglabs.zmq-async.core :refer [register-socket!]]
  (:require [fowles
             [admitter :as admitter]
             [util :as util]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.core.async :refer [>!! close!]]))

;; (def WORDS_FILE "/usr/share/dict/words")
;; (def NUM_WORDS 5)

(defn- enq-query-words-from-file
  ":: (str, chan) -> ()"
  [in-file start-date end-date to-ch]
  (util/line-by-line in-file
                     (fn [query]
                       (>!! to-ch {:query query
                                   :start-date start-date
                                   :end-date end-date})))
  (close! to-ch))

;;------------------------------

(defn admit-query-words-from-file
  ":: () -> chan"
  [in-file start-date end-date]
  (admitter/admit (partial enq-query-words-from-file
                           in-file start-date end-date)))

