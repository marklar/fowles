(ns fowles.cfg
  (:use [clojure.java.io])
  (:require [clojure.data.json :as json]))

(defn- load-cfg
  "If cfg file exists, return cfg data.
   If it does not, return default-cfg."
  [file-name]
  (if (.exists (as-file file-name))
    (let [json-str (slurp file-name)]
      (json/read-str json-str))
    (do
      (println "No configuration file found.  Using default config settings.")
      nil)))

(defn get-api-key []
  (if-let [cfg (load-cfg ".config.json")]
    (get cfg "api-key")))
