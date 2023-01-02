(ns app.interface.view.players
  (:require [re-frame.core :as rf]))

(defn player-card-view
  [{:keys [player-name owned-resources workers max-workers]}]
  (let [current-player-name (:player-name @(rf/subscribe [:current-player]))]
    [:div
     [:h3 player-name (if (= player-name current-player-name) " *" "")]
     (into [:div
             [:div (str "Workers: " workers "/" max-workers)]]
           (for [[resource-type quantity] (sort owned-resources)]
             [:div (str (name resource-type) ": " quantity)]))]))
