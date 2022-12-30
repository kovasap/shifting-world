(ns app.interface.map-generation
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [perlin2d.core :as p]
            [app.interface.utils :refer [get-only]]
            [clojure.string :as st]))

; TODO use one perlin noise for humidity and one for elevation to generate more
; land types in interesting ways

(def lands
  [{:letter     "F"
    :type       "forest"
    :perlin-cutoff 0.35
    :production {:wood 1}
    :style      {:background-color "green"}}
   {:letter     "P"
    :type       "plains"
    :perlin-cutoff 0.3
    :production {:food 1}
    :style      {:background-color "orange"}}
   {:letter     "W"
    :type       "water"
    :perlin-cutoff 0.0
    :production {:water 1}
    :style      {:background-color "blue"}}
   {:letter     "M"
    :type       "mountain"
    :perlin-cutoff 0.75
    :production {:rock 1}
    :style      {:background-color "grey"}}
   {:letter     "S"
    :type       "sand"
    :perlin-cutoff 0.2
    :production {:sand 1}
    :style      {:background-color "yellow"}}
   {:letter     "V"
    :type       "void"
    :perlin-cutoff 10.0
    :production {}
    :style      {:background-color "black"}}])


(defn get-land-below-perlin-cutoff
  [perlin-cutoff]
  (first (first (filter (fn [[_ nxt]] (> (:perlin-cutoff nxt) perlin-cutoff))
                  (partition 2 1 (sort-by :perlin-cutoff lands))))))

(assert (= (:type (get-land-below-perlin-cutoff 0.5)) "forest"))


; For a 12x12 map octaves freq amp of 1 0.08 2 seems to work well

(defn get-perlin-land
  [row-idx col-idx]
  (get-land-below-perlin-cutoff
    (let [octaves   8 ; The number of times the algorithm will run. A higher
                      ; number should result in more random looking results.
          frequency 0.05 ; should be from 0.0-1.0?
          amplitude 1]
      ; We add amplitude and divide by 2 to make sure all values are between 0
      ; and amplitude.
      (/ (+ amplitude
            (p/do-octave col-idx row-idx octaves frequency amplitude))
         2))))

(defn base-tile
  [args]
  (merge {:development      nil
          :row-idx          nil
          :col-idx          nil
          ; player
          :controller       nil
          :legal-placement? false
          ; nil if there is no worker
          :worker-owner     nil
          :land             nil}
         args))

(defn parse-board-str
  "Returns 2d array of tile maps."
  [board-str]
  (into []
        (map-indexed
          (fn [row-idx line]
            (into []
                  (map-indexed
                    (fn [col-idx tile-letter]
                      (base-tile 
                        {:row-idx    row-idx
                         :col-idx    col-idx
                         :land       (get-only lands :letter tile-letter)}))
                    (st/split (st/trim line) #" "))))
          (st/split-lines board-str))))

(def manual-board
  (parse-board-str
    "F F F F F F F F F F
     F F F F F F F F F F
     F F F F M F F F F F
     F F F F M M F F F F
     F F F M W M F F F F
     F F F W M F F F F F
     S S W W M M F F F F
     W W W W F F F F F F
     W W S S F F F F F F
     W W S S F F F F F F"))

(defn generate-perlin-board
  "Returns 2d array of tile maps."
  [width height]
  (into []
        (for [row-idx (range height)]
          (into []
                (for [col-idx (range width)]
                  (base-tile 
                    {:row-idx    row-idx
                     :col-idx    col-idx
                     :land       (get-perlin-land row-idx col-idx)}))))))
