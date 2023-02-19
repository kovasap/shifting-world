(ns app.interface.view.developments
  (:require [re-frame.core :as rf]
            [reagent.core :as r]))


(def dev-desc-hover-state (r/atom {}))
(defn development-desc-view
  [development row-idx col-idx]
  (let [unique-key [row-idx col-idx]]
    [:div {:style         {:width    "100%"
                           :height   "100%"
                           :position "absolute"
                           :z-index  1}
           :on-mouse-over #(swap! dev-desc-hover-state (fn [state]
                                                         (assoc state
                                                           unique-key true)))
           :on-mouse-out  #(swap! dev-desc-hover-state (fn [state]
                                                         (assoc state
                                                           unique-key false)))}
     [:div
      {:style {:position   "absolute"
               :background "white"
               :overflow   "visible"
               :text-align "left"
               :top        50
               :z-index    2
               :display    (if (get @dev-desc-hover-state unique-key)
                             "block"
                             "none")}}
      (:description development)]]))


(def unique-id (atom 1))
(defn development-blueprint-view
  [development]
  (let [n        (name (:type development))
        existing @(rf/subscribe [:developments (:type development)])
        current-player-name (:player-name @(rf/subscribe [:current-player]))
        placing  @(rf/subscribe [:placing])
        placing-current (= (:type placing) (:type development))]
    (swap! unique-id inc)
    [:div {:key   (str n @unique-id) ; Required by react (otherwise we get a warning).
           :style {:background "white" :text-align "left"
                   :width "150px"
                   :flex 1
                   :padding "15px"
                   :border "2px solid black"}}
     [:button.btn.btn-outline-primary
      {:style    {:font-weight (if placing-current "bold" "normal")}
       :on-click #(if placing-current
                    (rf/dispatch [:development/stop-placing])
                    (rf/dispatch [:development/start-placing
                                  (:type development)
                                  current-player-name]))}
      [:div "Place " n " " (count existing) "/" (:max development)]]
     [:div "(cost " (:cost development) ")"]
     [:div (:description development)]]))


(defn development-hand
  []
  (let [current-player @(rf/subscribe [:current-player])]
    (into [:div {:style {:display "flex" :gap "10px"}}]
          (for [development (:blueprints current-player)]
           (development-blueprint-view development)))))
