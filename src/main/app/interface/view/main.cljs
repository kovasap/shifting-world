(ns app.interface.view.main
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [clojure.string :as st]
            ; [ring.middleware.anti-forgery]
            [app.interface.sente :refer [chsk-state login]]
            [app.interface.view.map :refer [board-view]]
            [app.interface.view.orders :refer [order-view]]
            [app.interface.view.players :refer [player-card-view]]
            [app.interface.developments :refer [developments]]
            [app.interface.view.developments :refer [development-hand]]
            [cljs.pprint]))

(defn undo-button
  []
  ; only enable the button when there's undos
  (let [undos? (rf/subscribe [:undos?])]
    (fn []
      [:button.btn.btn-outline-primary
       {:disabled (not @undos?)
        :on-click #(rf/dispatch [:undo])}
       "Undo"])))


; Not currently necessary/used
(defn login-field
  []
  [:span
   [:input#input-login {:type :text :placeholder "User-id"}]
   [:button.btn.btn-outline-primary
    {:on-click (fn []
                 (let [user-id (.-value (.getElementById js/document
                                                         "input-login"))]
                   (login user-id)))}
    "Secure login!"]])


(defn main
  "Main view for the application."
  []
  (let [players @(rf/subscribe [:players])
        db      @(rf/subscribe [:db-no-board])
        orders  @(rf/subscribe [:orders])]
    [:div @chsk-state]
    [:div.container
     #_(let [csrf-token (force
                          ring.middleware.anti-forgery/*anti-forgery-token*)]
         [:div#sente-csrf-token {:data-csrf-token csrf-token}])
     [:h1 "Welcome to Terraforming Catan!"]
     ; [login-field]
     [:br]
     [:br]
     [:div {:style {:display  "grid"
                    :grid-template-columns "auto auto auto"
                    :grid-gap "15px"}}
      [:div
       [:button.btn.btn-outline-primary {:on-click #(rf/dispatch
                                                      [:game/setup])}
        "Setup Game"]
       [undo-button]
       (into [:div] (for [player players] (player-card-view player)))
       [:button.btn.btn-outline-primary {:on-click #(rf/dispatch [:end-turn])}
        "End Turn"]
       [:button.btn.btn-outline-primary {:on-click #(rf/dispatch [:end-round])}
        "End Round"]]
      [:div
       (board-view)
       [:br]
       [:div @(rf/subscribe [:message])]
       [:br]
       (development-hand)]
      (into [:div [:h1 "Orders"]] (for [order orders] (order-view order)))]
     [:div
      "TODO add diff of game state to show what just happened\n"
      [:pre (with-out-str (cljs.pprint/pprint db))]]]))



