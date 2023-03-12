(ns app.interface.view.developments
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [app.interface.utils :refer [get-only]]
            [app.interface.developments :refer [developments]]))


(def dev-desc-hover-state (r/atom {}))
(defn development-desc-view
  [development-type row-idx col-idx]
  (let [unique-key [row-idx col-idx]
        development (get-only developments :type development-type)]
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
        existing-num @(rf/subscribe [:num-developments (:type development)])
        current-player-name (:player-name @(rf/subscribe [:current-player]))
        placing  @(rf/subscribe [:placing])
        placing-current (= placing (:type development))]
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
      [:div "Place " n " " existing-num "/" (:max development)]]
     [:div (str "Land restriction: " (:valid-lands development))]
     [:div (str "Chains: " (:production-chains development))]
     [:div (:description development)]]))


(defn development-hand
  []
  (let [current-player @(rf/subscribe [:current-player])]
    (into [:div {:style {:display "flex" :gap "10px"}}]
          (for [development (:blueprints current-player)]
            (development-blueprint-view development)))))
