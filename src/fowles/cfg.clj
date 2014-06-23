(ns fowles.cfg
  "Access configuration info from file."
  (:use [clojure.java.io])
  (:require [clojure.data.json :as json]))

(def CFG_FILE_NAME ".config.json")

(defn- load-cfg
  "If cfg file exists, return cfg data.  Else, throw exception."
  []
  (if (.exists (as-file CFG_FILE_NAME))
    (let [json-str (slurp CFG_FILE_NAME)]
      (json/read-str json-str))
    (throw (Exception. "No configuration file found."))))

(def cfg)
(defn- get-cfg []
  (defonce cfg (load-cfg))
  cfg)

;;----------------------

(defn cfg-get [k]
  (get (get-cfg) (name k)))
