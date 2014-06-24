(ns fowles.cfg
  "Access configuration info from file."
  (:use [clojure.java.io])
  (:require [clojure.data.json :as json]))

(def CFG_FILE_NAME ".config.json")

(defn load-cfg
  "If cfg file exists, return cfg data.  Else, throw exception."
  [file-name]
  (if (.exists (as-file file-name))
    (let [json-str (slurp file-name)
          hmap     (json/read-str json-str)]
      (clojure.walk/keywordize-keys hmap))
    (throw (Exception. (str "Configuration file '"
                            file-name
                            "' NOT found.")))))

(def cfg)
(defn get-cfg []
  (defonce cfg (load-cfg CFG_FILE_NAME))
  cfg)

(defn cfg-get [k]
  (get (get-cfg) k))

;;----------------------

(defn- non-nil-or-throw
  [name v]
  (if-not (nil? v)
    v
    (throw (Exception. (str "Configuration error: `"
                            name
                            "` cannot be null.")))))

;;----------------------

(defn get-optional-field
  [config keywords]
  (get-in config keywords))

(defn get-required-field
  [config keywords]
  ;; (clojure.pprint/pprint config)
  (non-nil-or-throw
   (clojure.string/join "." (map name keywords))
   (get-in config keywords)))
