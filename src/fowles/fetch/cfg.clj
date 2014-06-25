(ns fowles.fetch.cfg
  "Access configuration info from file."
  (:require [fowles.cfg :as cfg]))

(def FILE_NAME "fetch_cfg.json")

(def file-cfg)
(defn get-cfg []
  (defonce file-cfg (cfg/load-cfg FILE_NAME))
  file-cfg)

(defn- gof [& keywords]
  (cfg/get-optional-field (get-cfg) keywords))

(defn- grf [& keywords]
  (cfg/get-required-field (get-cfg) keywords))

;;--------------------------

(defn api-key []
  (grf :uris :api_key))

(defn num-per-request []
  (grf :uris :num_ids_per_request))

(defn part []
  (grf :uris :args :part))

(defn fields []
  (grf :uris :args :fields))

(defn batch-size []
  (grf :concurrency :batches :num_requests))

(defn frequency-ms []
  (grf :concurrency :batches :frequency_ms))
  
(defn sleep-ms []
  (grf :concurrency :sleep_ms))

(defn in-port []
  (grf :ports :input))

(defn out-port []
  (grf :ports :output))

(defn in-file []
  (gof :files :input))

(defn out-file []
  (gof :files :output))

(defn failed-file []
  (grf :files :failed))

;; (defn log-file []
;;   (grf :files :log))

(defn validate []
  "If valid, return nil.
   Else throw Exception."
  (doseq [f [api-key
             num-per-request
             part
             fields
             batch-size
             frequency-ms
             sleep-ms
             ;; in-file
             ;; out-file
             in-port
             out-port
             failed-file
             ;; log-file
             ]]
    (f)))
