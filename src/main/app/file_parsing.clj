(ns app.file-parsing
  (:require [clojure.java.io]))

(defn parse-files
  [root-dir]
  (mapv str (filter #(.isFile %)
                    (file-seq (clojure.java.io/file root-dir)))))
