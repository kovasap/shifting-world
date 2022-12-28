(ns app.interface.core
  (:require ["react-dom/client" :refer [createRoot]]
            [ajax.core :as ajax]
            [app.config :as config]
            [day8.re-frame.http-fx]
            [goog.dom :as gdom]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [app.interface.utils :refer [get-only]]
            [clojure.string :as st]
            [app.interface.board :refer [render-board update-tiles]]
            [app.interface.developments :refer [developments update-resources]]
            [cljs.pprint]
            [taoensso.timbre :as log]))

(def resource-types
  [:wood :water :sand])

(defn render-player-card
  [{:keys [player-name resources workers max-workers]}]
  (let [current-player-name @(rf/subscribe [:current-player-name])]
    [:div
     [:h3 player-name (if (= player-name current-player-name) " *" "")]
     (into [:div
             [:div (str "Workers: " workers "/" max-workers)]]
           (for [resource resource-types]
             [:div (str (name resource) ": " (resource resources))]))]))
  

(defn- main
  "Main view for the application."
  []
  (let [experiments @(rf/subscribe [:experiments])
        players     @(rf/subscribe [:players])
        placing     @(rf/subscribe [:placing])
        db  @(rf/subscribe [:db-no-board])
        current-player-name @(rf/subscribe [:current-player-name])]
    [:div.container
     [:h1 "Welcome to Terraforming Catan!"]
     [:button.btn.btn-outline-primary {:on-click #(rf/dispatch [:game/setup])}
      "Setup Game"]
     [:br]
     [:br]
     [:div {:style {:display  "grid"
                    :grid-template-columns "auto auto auto"
                    :grid-gap "15px"}}
      (into [:div] (for [player players] (render-player-card player)))
      [:div
       (render-board)
       [:br]
       [:div @(rf/subscribe [:message])]
       [:br]
       (doall
         (for [development developments
               :let        [n        (name (:type development))
                            existing @(rf/subscribe [:developments
                                                     (:type development)])
                            placing-current (= (:type placing)
                                               (:type development))]]
           [:button.btn.btn-outline-primary
            {:key      n ; Required by react (otherwise we get a warning).
             :style    {:font-weight (if placing-current
                                       "bold"
                                       "normal")}
             :on-click #(if placing-current
                          (rf/dispatch [:development/stop-placing])
                          (rf/dispatch [:development/start-placing
                                        (:type development)
                                        current-player-name]))}
            [:div "Place " n " " (count existing) "/" (:max development)]
            [:div "(cost " (:cost development) ")"]]))
       [:button.btn.btn-outline-primary {:on-click #(rf/dispatch [:end-turn])}
        "End Turn"]
       [:button.btn.btn-outline-primary {:on-click #(rf/dispatch [:end-round])}
        "End Round"]]
      [:div "TODO add diff of game state to show what just happened\n"
       [:pre (with-out-str (cljs.pprint/pprint db))]]]]))


(rf/reg-sub
  :db-no-board
  (fn [db _]
    (dissoc db :board)))


;; ----------------------------------------------------------------------------
;; Setup

(defn player-data
  [i player-name]
  {:player-name player-name
   :index i
   :color (get ["blue" "red" "purple" "black"] i)
   :workers 2
   :max-workers 2
   :resources   (into {} (for [t resource-types] [t 1]))})

(rf/reg-event-db
  :game/setup
  (fn [db _]
    (rf/dispatch [:board/setup])
    (assoc db
      :message ""
      :players (into [] (map-indexed player-data ["cupid" "zeus" "hades"]))
      :current-player-idx 0
      :placing false)))

(rf/reg-event-db
  :message
  (fn [db [_ message]]
    (assoc db :message message)))

(rf/reg-sub
  :message
  (fn [db _]
    (:message db)))

(rf/reg-sub
  :players
  (fn [db _]
    (:players db)))

(rf/reg-sub
  :current-player-name
  (fn [{:keys [players current-player-idx] :as db} _]
    (:player-name (nth players current-player-idx))))

(defn next-player-idx
  [{:keys [players current-player-idx] :as db}]
  (if (= (+ 1 current-player-idx) (count players))
    0
    (+ 1 current-player-idx)))

;; ----------------------------------------------------------------------------
;; End of Turn

(rf/reg-event-db
  :end-turn
  (fn [db [_]]
    (-> db
        (assoc :current-player-idx (next-player-idx db)))))


(rf/reg-event-db
  :end-round
  (fn [db [_]]
    (-> db
        (update :players
                (fn [players]
                  (into []
                        (for [player players]
                          (assoc player :workers (:max-workers player))))))
        (update :board
                update-tiles
                (fn [tile] (assoc tile :worker-owner nil))))))


;; -----------------------------------------------------------------------------
;; Events and Subscriptions to query the backend and store the result in the
;; app-state.

(rf/reg-event-fx
 :experiments/get
 (fn [_ _]
   {:fx [[:http-xhrio {:method :get
                       :uri (str config/api-location "/experiments")
                       :format (ajax/transit-request-format)
                       :response-format (ajax/transit-response-format)
                       :on-success [:experiments.get/success]
                       :on-failure [:experiments.get/error]}]]}))

(rf/reg-event-db
 :experiments.get/success
 (fn [db [_ response]]
   (assoc db :experiments (:experiments response))))

(rf/reg-event-fx
  :experiments.get/error
  (fn [_ [_ error]]
    {:fx
       [[:log/error
         (str
           "Could not query the experiments. Did you forget to start the api? "
           error)]]}))

(rf/reg-sub
 :experiments
 (fn [db _]
   (:experiments db)))



(rf/reg-fx
 :log/error
 (fn [message]
   (log/error message)))



;; -- Entry Point -------------------------------------------------------------

(defonce root (createRoot (gdom/getElement "app")))

(defn init
  []
  (rf/dispatch [:game/setup])
  (.render root (r/as-element [main])))

(defn- ^:dev/after-load re-render
  "The `:dev/after-load` metadata causes this function to be called after
  shadow-cljs hot-reloads code. This function is called implicitly by its
  annotation."
  []
  (rf/clear-subscription-cache!)
  (init))
