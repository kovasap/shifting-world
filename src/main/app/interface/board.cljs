(ns app.interface.board
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [app.interface.config :refer [debug]]
            [app.interface.map-generation :refer [manual-board
                                                  generate-perlin-board]]))


(defn update-tiles
  [board update-fn]
  (into []
        (for [column board] (into [] (for [tile column] (update-fn tile))))))

(rf/reg-event-db
  :board/setup
  (fn [db _]
    (assoc db :board manual-board)))
    ; (assoc db :board (generate-perlin-board 15 10))))

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

(defn one-away?
  [n1 n2]
  (or (= n1 (dec n2))
      (= n1 (inc n2))))

(defn adjacent?
  "Checks if two tiles are adjacent or not (returns a bool)."
  [{row-idx1 :row-idx col-idx1 :col-idx} {row-idx2 :row-idx col-idx2 :col-idx}]
  (or
    ; Same row
    (and (= row-idx1 row-idx2) (one-away? col-idx1 col-idx2))
    (and (one-away? row-idx1 row-idx2)
         (or (and (even? row-idx1) (or (= col-idx1 col-idx2)
                                       (= col-idx1 (inc col-idx2))))
             (and (odd? row-idx1) (or (= col-idx1 col-idx2)
                                      (= col-idx1 (dec col-idx2))))))))
  

(defn get-adjacent-tiles
  [board tile]
  (reduce concat (for [column board] (filter #(adjacent? % tile) column))))

(rf/reg-sub
  :adjacent-tiles
  (fn [db [_ tile]]
    (get-adjacent-tiles (:board db) tile)))


(defn update-adjacent-tiles
  [board tile update-fn]
  (update-tiles board (fn [t] (if (adjacent? t tile) (update-fn t) t))))

;; -------------------------------------------------------------------------
;; View

; See resources/public/css/board.css for supporting css.
; TODO make better when
; https://github.com/schnaq/cljs-re-frame-full-stack/issues/1 is fixed
(def tile-hover-state (r/atom {}))
(defn render-tile
  [{:keys [land
           row-idx
           col-idx
           development
           legal-placement?
           worker-owner
           controller]
    :as   tile}]
  (let [adjacent-tiles @(rf/subscribe [:adjacent-tiles tile])
        hovered (get-in @tile-hover-state [row-idx col-idx])]
    [:div.tile
     {:style         {:font-size "10px"
                      :text-align "center"}
      :on-mouse-over #(doseq [{t-row-idx :row-idx t-col-idx :col-idx}
                              (conj adjacent-tiles tile)]
                        (swap! tile-hover-state
                             (fn [state]
                               (assoc-in state
                                 [t-row-idx t-col-idx]
                                 true))))
      :on-mouse-out #(doseq [{t-row-idx :row-idx t-col-idx :col-idx}
                             (conj adjacent-tiles tile)]
                       (swap! tile-hover-state
                            (fn [state]
                              (assoc-in state
                                [t-row-idx t-col-idx]
                                false))))
      :on-click      #(cond @(rf/subscribe [:placing]) (rf/dispatch
                                                         [:development/place
                                                          tile])
                            development (rf/dispatch [:development/use
                                                      development
                                                      tile])
                            :else (rf/dispatch [:message
                                                "Can't do anything here"]))}
     [:div.background
      {:style (merge (:style land)
                     {:width    "100%"
                      :height   "100%"
                      :position "absolute"
                      :z-index  -1
                      :opacity  (cond (and legal-placement? hovered) 1.0
                                      legal-placement? 0.8
                                      :else 0.7)})}]
     [:div {:style {:padding-top "10px"}
            :display (if debug "none" "block")} row-idx ", " col-idx]
     [:div {:style {:color (:color controller)}}
      (if controller (str (:player-name controller) "'s") nil)]
     [:div (if development (name (:type development)) nil)]
     [:div (if worker-owner worker-owner nil)]]))


; Defined as --s and --m in resources/public/css/board.css.  These values must
; be kept in sync!
(def hex-tile-size-px 100)
(def hex-margin-px 4)
(defn required-hex-grid-px-width
  [board]
  (let [board-cols (count board)]
    (+ (* 2 hex-margin-px board-cols)
       ; add 1 to board-cols here to make sure that every row has the same
       ; number of hexes
       (* hex-tile-size-px (+ 1 board-cols)))))

(defn render-board
  []
  (let [board @(rf/subscribe [:board])]
    [:div.boardmain
      (into
        [:div.board
         {:style {:width (str (required-hex-grid-px-width board) "px")}}]
        (reduce concat
          (for [column board] (for [tile column] (render-tile tile)))))]))
