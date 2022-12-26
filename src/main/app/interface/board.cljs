(ns app.interface.board
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

(def board-str
  "F F F F F F F F F F
   F F F F F F F F F F
   F F F F M F F F F F
   F F F F M M F F F F
   F F F M W M F F F F
   F F F W M F F F F F
   S S W W M M F F F F
   W W W W F F F F F F
   W W S S F F F F F F
   W W S S F F F F F F")

(defn base-tile
  [args]
  (merge {:development      nil
          :row-idx          nil
          :col-idx          nil
          :legal-placement? false
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


(defn update-tiles
  [board update-fn]
  (into []
        (for [column board] (into [] (for [tile column] (update-fn tile))))))
  
 

(rf/reg-event-db
  :board/setup
  (fn [db _]
    ; (assoc db :board (parse-board-str board-str))
    (assoc db :board (generate-perlin-board 15 15))))

(rf/reg-sub
  :board
  (fn [db _]
    (:board db)))

(rf/reg-sub
  :developments
  (fn [db _]
    (reduce concat
      (for [column (:board db)]
        (for [tile column] (:development tile))))))

;; -------------------------------------------------------------------------
;; View

; See resources/public/css/board.css for supporting css.
; TODO make better when
; https://github.com/schnaq/cljs-re-frame-full-stack/issues/1 is fixed
(def tile-hover-state (r/atom {}))
(defn render-tile
  [{:keys [land row-idx col-idx development legal-placement?] :as tile}]
  (let [hovered (get-in @tile-hover-state [row-idx col-idx])]
    [:div.tile {:style         (merge
                                 (:style land)
                                 {:opacity (cond
                                             (and legal-placement? hovered) 1.0
                                             legal-placement? 0.9
                                             :else 0.7)})
                :on-mouse-over #(swap! tile-hover-state (fn [state]
                                                          (assoc-in state
                                                            [row-idx col-idx]
                                                            true)))
                :on-mouse-out  #(swap! tile-hover-state (fn [state]
                                                          (assoc-in state
                                                            [row-idx col-idx]
                                                            false)))
                :on-click      #(if legal-placement?
                                  (rf/dispatch [:development/place tile])
                                  nil)}
     (if development development nil)]))
(defn render-board
  []
  (let [board @(rf/subscribe [:board])]
    (into
      [:div.board
       ; https://www.w3schools.com/css/css_grid.asp
       {:style {:grid-template-columns (st/join " "
                                                (repeat (count (first board))
                                                        "max-content"))}}]
      (reduce concat
        (for [column board] (for [tile column] (render-tile tile)))))))
