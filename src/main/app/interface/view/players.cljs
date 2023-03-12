(ns app.interface.view.players
  (:require [re-frame.core :as rf]))

(defn player-card-view
  [{:keys [player-name]}]
  (let [current-player-name (:player-name @(rf/subscribe [:current-player]))]
    [:div [:h3 player-name (if (= player-name current-player-name) " *" "")]]))
