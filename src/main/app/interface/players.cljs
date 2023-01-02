(ns app.interface.players
  (:require [re-frame.core :as rf]
            [app.interface.resources :refer [resources]]))

(defn update-resources
  [db player-idx resource-delta]
  (update-in db
             [:players player-idx :owned-resources]
             #(merge-with + resource-delta %)))

(defn update-resources-with-check
  [db player-idx resource-delta]
  (let [updated (update-resources db player-idx resource-delta)]
    (if (seq (filter (fn [[_ amount]] (> 0 amount))
               (get-in updated [:players player-idx :owned-resources])))
      [false (assoc db :message "Cannot pay the cost!")]
      [true updated])))

(defn player-data
  [i player-name]
  {:player-name player-name
   :index i
   :color (get ["blue" "red" "purple" "black"] i)
   :workers 2
   :max-workers 2
   :owned-resources   (assoc (into {} (for [{:keys [type]} resources] [type 0]))
                            :stone 5)})

(rf/reg-sub
  :players
  (fn [db _]
    (:players db)))

(defn get-current-player
  [{:keys [players current-player-idx] :as db}]
  (nth players current-player-idx))

(rf/reg-sub
  :current-player
  (fn [db _]
    (get-current-player db)))

(defn next-player-idx
  [{:keys [players current-player-idx] :as db}]
  (if (= (+ 1 current-player-idx) (count players))
    0
    (+ 1 current-player-idx)))


