(ns app.interface.scoring
  (:require
   [re-frame.core :as rf]
   [app.interface.developments :refer [developments]]
   [app.interface.utils :refer [get-only]]
   [app.interface.board :refer [get-adjacent-tiles]]))

; TODO also add points for use of opponents resources
(defn get-opponent-adjacency-points
  [{:keys [board]} player-name]
  (reduce +
    (flatten
      (for [tile (flatten board)]
        (for [{:keys [controller-name]} (get-adjacent-tiles board tile)
              :when (= player-name (:controller-name tile))]
          (if (or (nil? controller-name) (= controller-name player-name))
            0
            1))))))

(defn get-development-points
  [{:keys [board]} player-name]
  (reduce +
    (for [{:keys [controller-name development-type]} (flatten board)]
      (let [{:keys [points]} (get-only developments :type development-type)]
        (if (= controller-name player-name)
          points
          0)))))

(defn get-produced-points
  [{:keys [board]} player-name]
  (reduce +
    (for [{:keys [production controller-name]} (flatten board)]
      (if (= controller-name player-name)
        (get production :points 0)
        0))))

(rf/reg-sub
  :score-for-player
  (fn [db [_ player-name]]
    {:adjacency (get-opponent-adjacency-points db player-name)
     :developments (get-development-points db player-name)
     :production (get-produced-points db player-name)}))
    
