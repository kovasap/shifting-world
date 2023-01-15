(ns app.interface.board
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [app.interface.config :refer [debug]]))

(defn update-tiles
  [board update-fn]
  (into []
        (for [column board] (into [] (for [tile column] (update-fn tile))))))


(defn update-board-tile
  "Returns a new board with the first tile in the list updated."
  [board tiles update-fn]
  (if (empty? tiles)
    board
    (let [{:keys [row-idx col-idx]} (first tiles)]
        (-> board
          (update-in [row-idx col-idx] (partial update-fn board))
          (update-board-tile (rest tiles) update-fn)))))
  

(defn update-board-tiles
  "Feeds the updated board as an additional argument to each call of update-fn."
  [board update-fn]
  (update-board-tile board (reduce concat board) update-fn))

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

(defn adjacent-to-owned-developments?
  [board tile player]
  (some #(= (:player-name (:controller %)) (:player-name player))
        (get-adjacent-tiles board tile)))

(defn update-adjacent-tiles
  [board tile update-fn]
  (update-tiles board (fn [t] (if (adjacent? t tile) (update-fn t) t))))
