(ns app.interface.players
  (:require [re-frame.core :as rf]
            [app.interface.resources :refer [resources]]))

(defn render-player-card
  [{:keys [player-name owned-resources workers max-workers]}]
  (let [current-player-name (:player-name @(rf/subscribe [:current-player]))]
    [:div
     [:h3 player-name (if (= player-name current-player-name) " *" "")]
     (into [:div
             [:div (str "Workers: " workers "/" max-workers)]]
           (for [[resource-type quantity] (sort owned-resources)]
             [:div (str (name resource-type) ": " quantity)]))]))
  


(defn player-data
  [i player-name]
  {:player-name player-name
   :index i
   :color (get ["blue" "red" "purple" "black"] i)
   :workers 2
   :max-workers 2
   :owned-resources   (into {} (for [{:keys [type]} resources] [type 1]))})

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


