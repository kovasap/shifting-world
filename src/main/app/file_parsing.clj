(ns app.file-parsing
  (:require [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [clojure.string :refer [split replace]]))

(def FilePath :string)

(def Wormlist
  [:sequential
   [:map [:x :int]
         [:y :int]
         [:frames-alive :int]
         [:id :int]
         [:days-alive :double]
         [:mins-alive :int]]])
  

(def Experiment
  [:map [:name :string]
        [:id :int]
        [:filepath FilePath]
        [:wormlist Wormlist]
        [:start-timestamp :int]
        [:num-frames :int]])

(defn get-experiment-dirs
  {:malli/schema [:=> [:cat FilePath] [:sequential FilePath]]}
  [root-dir]
  (mapv str (filter #(and (.isDirectory %)
                          (not (= root-dir (str %))))
                    (file-seq (io/file root-dir)))))

; TODO use malli to coerce the types to non-strings are defined in Experiment
(defn parse-description-txt
  {:malli/schema [:=> [:cat FilePath] Experiment]}
  [filepath]
  (if (.exists (io/file filepath))
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
            (assoc :id (replace (:id-str experiment) #"expID:" ""))))
    {}))


(defn parse-wormlist
  {:malli/schema [:=> [:cat FilePath] Wormlist]}
  [filepath]
  (if (.exists (io/file filepath))
    (with-open [reader (io/reader filepath)]
      (into []
            (for [row (csv/read-csv reader)]
              (zipmap [:x :y :frames-alive :id :days-alive :mins-alive]
                      row))))
    []))
    

(defn parse-experiment-dir
  {:malli/schema [:=> [:cat FilePath] Experiment]}
  [expr-dir]
  (assoc (parse-description-txt (str expr-dir "/description.txt"))
         :wormlist (parse-wormlist (str expr-dir "/wormlist.txt"))))

(defn parse-experiments
  {:malli/schema [:=> [:cat FilePath] [:sequential Experiment]]}
  [root-dir]
  (for [dir #p (get-experiment-dirs root-dir)]
    (parse-experiment-dir dir)))

(defn parse-files
  [root-dir]
  (mapv str (filter #(.isFile %)
                    (file-seq (clojure.java.io/file root-dir)))))
