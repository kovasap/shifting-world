(ns app.interface.view.developments
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [clojure.string :as st]
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
  (let [dev-name            (name (:type development))
        existing-num        @(rf/subscribe [:num-developments
                                            (:type development)])
        current-player-name (:player-name @(rf/subscribe [:current-player]))
        placing             @(rf/subscribe [:placing])
        placing-current     (= placing (:type development))]
    (swap! unique-id inc)
    [:div {:key      (str dev-name @unique-id) ; Required by react (otherwise
                                               ; we get a warning).
           :style    {:background  "LightBlue"
                      :text-align  "left"
                      :width       "250px"
                      :height      "250px"
                      :flex        1
                      :padding     "15px"
                      :font-weight (if placing-current "bold" "normal")
                      :border      "2px solid black"}
           :on-click #(if placing-current
                        (rf/dispatch [:development/stop-placing])
                        (rf/dispatch [:development/start-placing
                                      (:type development)
                                      current-player-name]))}
     [:div [:strong dev-name] " " existing-num "/" (:max development)]
     [:div
      [:small
       "Place in "
       (st/join ", " (sort (map name (:valid-lands development))))]]
     [:div
      "Chains: "
      (for [chain (:production-chains development)]
        [:div {:key (str chain @unique-id)}
         (str chain)])]
     [:div (:description development)]]))


(defn blueprints
  []
  [:div
    [:div "The game ends when all developments are placed!"]
    [:br]
    (into [:div {:style {:display  "grid"
                         :grid-template-columns "auto auto"
                         :margin-bottom "100%"
                         :grid-gap "10px"}}]
          (for [development (sort-by :type @(rf/subscribe [:blueprints]))]
            (development-blueprint-view development)))])
