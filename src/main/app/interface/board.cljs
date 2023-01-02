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

(defn adjacent-to-owned-developments?
  [board tile player]
  (some #(= (:player-name (:controller %)) (:player-name player))
        (get-adjacent-tiles board tile)))

(defn update-adjacent-tiles
  [board tile update-fn]
  (update-tiles board (fn [t] (if (adjacent? t tile) (update-fn t) t))))
