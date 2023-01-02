(ns app.interface.view.developments
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [app.interface.developments :refer [developments]]))


(def dev-card-hover-state (r/atom {}))
(defn development-card-view
  [development]
  [:div {:style         {:width "100%" :height "100%" :position "absolute"
                         :z-index 1}
         :on-mouse-over #(swap! dev-card-hover-state (fn [state]
                                                       (assoc state
                                                         development true)))
         :on-mouse-out  #(swap! dev-card-hover-state (fn [state]
                                                       (assoc state
                                                         development false)))}
   [:div
    {:style {:position   "absolute"
             :background "white"
             :overflow   "visible"
             :text-align "left"
             :top 35
             :z-index    2
             :display    (if (get @dev-card-hover-state development)
                           "block"
                           "none")}}
    (:description development)]])
