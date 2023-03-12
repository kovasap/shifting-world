(ns app.interface.view.players
  (:require [re-frame.core :as rf]))

(defn player-card-view
  [{:keys [player-name color points]}]
  (let [current-player-name (:player-name @(rf/subscribe [:current-player]))]
    [:div
     [:div {:style {:color color}}
      player-name
      (if (= player-name current-player-name) "*" "")]
     [:div points " pts"]]))
