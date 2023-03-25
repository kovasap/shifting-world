(ns app.interface.scoring
  (:require
   [re-frame.core :as rf]
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

(rf/reg-sub
  :score-for-player
  (fn [db [_ player-name]]
     (get-opponent-adjacency-points db player-name)))
    
