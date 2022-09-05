(ns app.file-parsing
  (:require [clojure.java.io]
            [clojure.string :refer [split replace]]))

(def FilePath :string)

(def Experiment
  [:map [:name :string
         :id :int
         :filepath FilePath
         :start-timestamp :int
         :num-frames :int]])

(defn get-experiment-dirs
  {:malli/schema [:=> [:cat FilePath] [:sequential FilePath]]}
  [root-dir]
  (prn root-dir)
  (mapv str (filter #(and (.isDirectory %)
                          (not (= root-dir (str %))))
                    (file-seq (clojure.java.io/file root-dir)))))

; TODO use malli to coerce the types to non-strings are defined in Experiment
(defn parse-description-txt
  [filepath]
  (let [experiment (zipmap
                     [:blank
                      :name
                      :blank
                      :blank
                      :blank
                      :start-timestamp
                      :num-frames
                      :blank
                      :???
                      :filepath
                      :???
                      :???
                      :id-str
                      :x-coord
                      :y-coord
                      :plate-pos
                      :well-pos
                      :???
                      :???]
                     (split (slurp filepath) #"\n"))]
      (-> experiment
          (dissoc :blank :???)
          (assoc :id (replace (:id-str experiment) #"expID:" "")))))
    

(defn parse-experiment-dir
  {:malli/schema [:=> [:cat FilePath] Experiment]}
  [expr-dir]
  (parse-description-txt (str expr-dir "/description.txt")))

(defn parse-experiments
  {:malli/schema [:=> [:cat FilePath] [:sequential Experiment]]}
  [root-dir]
  (for [dir #p (get-experiment-dirs root-dir)]
    (parse-experiment-dir dir)))

(defn parse-files
  [root-dir]
  (mapv str (filter #(.isFile %)
                    (file-seq (clojure.java.io/file root-dir)))))
