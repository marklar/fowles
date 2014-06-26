(ns fowles.fetch.cfg
  "Access configuration info from file."
  (:require [fowles.cfg :as cfg]))

(def FILE_NAME "config/fetch_cfg.json")

(def file-cfg)
(defn get-cfg []
  (defonce file-cfg (cfg/load-cfg FILE_NAME))
  file-cfg)

(defn- grf [& keywords]
  (cfg/get-required-field (get-cfg) keywords))

;;--------------------------

(defn in-host []
  (grf :servers :input :host))
(defn in-port []
  (grf :servers :input :port))

(defn out-host []
  (grf :servers :output :host))
(defn out-port []
  (grf :servers :output :port))

(defn failed-host []
  (grf :servers :failed :host))
(defn failed-port []
  (grf :servers :failed :port))

;;---------------

(defn num-per-request []
  (grf :requests :num_ids_per_request))

(defn part []
  (grf :requests :args :part))

(defn fields []
  (grf :requests :args :fields))

;;--------------

(defn batch-size []
  (grf :concurrency :batches :num_requests))

(defn frequency-ms []
  (grf :concurrency :batches :frequency_ms))
  
(defn sleep-ms []
  (grf :concurrency :sleep_ms))

;;--------------

(defn validate []
  "If valid, return nil.
   Else throw Exception."
  (doseq [f [num-per-request
             part
             fields
             batch-size
             frequency-ms
             sleep-ms
             in-host in-port
             out-host out-port
             failed-host failed-port
             ;; log-file
             ]]
    (f)))
