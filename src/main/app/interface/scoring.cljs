(ns app.interface.scoring
  (:require
   [re-frame.core :as rf]
   [app.interface.developments :refer [developments]]
   [app.interface.utils :refer [get-only]]
   [app.interface.board :refer [get-adjacent-tiles]]))

; TODO also add points for use of opponents resources
(defn get-opponent-adjacency-points
  [{:keys [board]} player-idx]
  (reduce +
    (flatten
      (for [tile (flatten board)]
        (for [{:keys [controller-idx]} (get-adjacent-tiles board tile)
              :when (= player-idx (:controller-idx tile))]
          (if (or (nil? controller-idx) (= controller-idx player-idx))
            0
            1))))))

(defn get-development-points
  [{:keys [board]} player-idx]
  (reduce +
    (for [{:keys [controller-idx development-type]} (flatten board)]
      (let [{:keys [points]} (get-only developments :type development-type)]
        (if (= controller-idx player-idx)
          points
          0)))))

(defn get-produced-points
  [{:keys [board]} player-idx]
  (reduce +
    (for [{:keys [production controller-idx]} (flatten board)]
      (if (= controller-idx player-idx)
        (get production :points 0)
        0))))

(rf/reg-sub
  :score-for-player
  (fn [db [_ player-idx]]
    {:adjacency (get-opponent-adjacency-points db player-idx)
     :developments (get-development-points db player-idx)
     :production (get-produced-points db player-idx)}))
    
