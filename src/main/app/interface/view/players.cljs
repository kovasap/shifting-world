(ns app.interface.view.players
  (:require [re-frame.core :as rf]))

(defn player-card-view
  [{:keys [player-name color]}]
  (let [current-player-name (:player-name @(rf/subscribe [:current-player]))
        points @(rf/subscribe [:score-for-player player-name])]
    [:div
     [:div {:style {:color color}}
      player-name
      (if (= player-name current-player-name) "*" "")]
     (for [[k v] points]
       [:div {:key (str player-name (name k))} v " pts for " (name k)])]))
