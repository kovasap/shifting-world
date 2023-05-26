(ns app.interface.scoring
  (:require
   [re-frame.core :as rf]
   [app.interface.board :refer [get-adjacent-tiles]]))

(defn get-opponent-adjacency-points
  [{:keys [board]} player-idx]
  (reduce +
    (flatten
      (for [tile (flatten board)
            :when (= player-idx (:controller-idx tile))]
        (for [{:keys [controller-idx]} (get-adjacent-tiles board tile)
              :when (and (not (nil? controller-idx))
                         (not (= controller-idx player-idx)))]
          2)))))

(defn get-produced-points
  [{:keys [board]} player-idx]
  (reduce +
    (for [{:keys [production controller-idx]} (flatten board)]
      (if (= controller-idx player-idx)
        (get production :points 0)
        0))))

(defn get-largest-area-points
  [{:keys [board]} player-idx]
  0)


(rf/reg-sub
  :score-for-player
  (fn [db [_ player-idx]]
    {:adjacency (get-opponent-adjacency-points db player-idx)
     :production (get-produced-points db player-idx)
     :largest-area (get-largest-area-points db player-idx)}))
    
