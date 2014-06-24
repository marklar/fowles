(ns fowles.search.query.admitter
  "Take in whatever input is required to perform data collection.
   + Source: from wherever (zmq socket?).
   + Data:   video-ids, queries.
   + Output: Put for requester."
  ;; [com.keminglabs.zmq-async.core :refer [register-socket!]]
  (:require [fowles.admitter :as admitter]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.core.async :refer [>!! close!]]))

;; (def WORDS_FILE "/usr/share/dict/words")
;; (def NUM_WORDS 5)

(defn- enq-query-words
  ":: (str, chan) -> ()"
  [in-file start-date end-date to-ch]
  ;; FIXME: The problem: NOT lazy!
  (let [queries (str/split-lines (slurp in-file))]
    ;; (admitter/enq to-ch (take NUM_WORDS words)))
    ;; (admitter/enq to-ch queries))
    (admitter/enq to-ch (map (fn [q] {:query q
                                      :start-date start-date
                                      :end-date end-date})
                             queries)))
  (close! to-ch))

;;------------------------------

(defn admit-query-words-from-file
  ":: () -> chan"
  [in-file start-date end-date]
  (admitter/admit (partial enq-query-words in-file start-date end-date)))

