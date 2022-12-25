(ns app.interface.board
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [app.interface.utils :refer [get-only]]
            [clojure.string :as st]))


(def land-types
  [{:letter "F" :type "forest" :production {:wood 1}}
   {:letter "W" :type "water" :production {:water 1}}
   {:letter "S" :type "sand" :production {:sand 1}}])


(def board-str
  "F F F F F F F F
   F F F W F F F F
   F F F W W F F F
   F F F F W F F F
   S S S S S F F F")

(defn parse-board-str
  [board-str]
  (into []
        (map-indexed
          (fn [row-idx line]
            (into []
                  (map-indexed
                    (fn [col-idx tile-letter]
                      {:structures []
                       :row-idx    row-idx
                       :col-idx    col-idx
                       :land       (get-only land-types :letter tile-letter)})
                    (st/split (st/trim line) #" "))))
          (st/split-lines board-str))))

(rf/reg-event-db
  :board/setup
  (fn [db _]
    (assoc db :board (parse-board-str board-str))))

(rf/reg-sub
  :board
  (fn [db _]
    (:board db)))

(rf/reg-sub
  :structures
  (fn [db _]
    (reduce concat
      (for [column (:board db)]
        (reduce concat (for [tile column] (:structures tile)))))))

;; -------------------------------------------------------------------------
;; View

; See resources/public/css/board.css for supporting css.
; TODO make better when
; https://github.com/schnaq/cljs-re-frame-full-stack/issues/1 is fixed
(def tile-hover-state (r/atom {}))
(defn render-tile
  [{:keys [land row-idx col-idx structures] :as tile}]
  (let [placing @(rf/subscribe [:placing])
        hovered (get-in @tile-hover-state [row-idx col-idx])]
    [:div.tile {:class         (:type land)
                :style         {:opacity (cond (and placing hovered) 1.0
                                               placing 0.8
                                               :else 0.7)}
                :on-mouse-over #(swap! tile-hover-state (fn [state]
                                                          (assoc-in state
                                                            [row-idx col-idx]
                                                            true)))
                :on-mouse-out  #(swap! tile-hover-state (fn [state]
                                                          (assoc-in state
                                                            [row-idx col-idx]
                                                            false)))
                :on-click      #(if placing
                                  (rf/dispatch [:place/tile tile])
                                  nil)}
     (str structures)]))
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
