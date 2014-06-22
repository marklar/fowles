(ns fowles.search.query.admitter
  "Take in whatever input is required to perform data collection.
   + Source: from wherever (zmq socket?).
   + Data:   video-ids, queries.
   + Output: Put for requester."
  ;; [com.keminglabs.zmq-async.core :refer [register-socket!]]
  (:require [fowles.admitter :as admitter]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def WORDS_FILE "/usr/share/dict/words")
(def NUM_WORDS 5)

(defn- enq-query-words
  ":: chan -> ()"
  [to-ch]
  (let [words (str/split-lines (slurp WORDS_FILE))]
    (admitter/enq to-ch (take NUM_WORDS words))))
  ;; (with-open [rdr (io/reader WORDS_FILE)]
  ;;   (doseq [word (line-seq rdr)]
  ;;     (println word)
  ;;     (>!! to-ch word))
  ;;   (close! to-ch)))

;;------------------------------

(defn admit-query-words
  ":: () -> chan"
  []
  (admitter/admit enq-query-words))

