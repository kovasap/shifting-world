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
            [app.interface.board :refer [render-board]]
            [taoensso.timbre :as log]))

(def resource-types
  [:wood :water :sand])

(def structures
  [{:type :settlement :production {}} {:type :bridge :production {}}])

(defn render-player-card
  [{:keys [player-name resources]}]
  (let [current-player-name @(rf/subscribe [:current-player-name])]
    [:div
     [:h3 player-name (if (= player-name current-player-name) " *" "")]
     (into [:div]
           (for [resource resource-types]
             [:div (str (name resource) ": " (resource resources))]))]))
  

(defn- main
  "Main view for the application."
  []
  (let [experiments @(rf/subscribe [:experiments])
        players     @(rf/subscribe [:players])
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
       (for [n (map :type structures)]
         [:button.btn.btn-outline-primary
          {:key      (name n) ; Required by react (otherwise we get a
                              ; warning).
           :on-click #(rf/dispatch [:place/start n current-player-name])}
          (str "Place " (name n))])
       [:button.btn.btn-outline-primary {:on-click #(rf/dispatch [:end-turn])}
        "End Turn"]]
      [:div "TODO add diff of game state to show what just happened"]]]))
     ; (when wizard [:p.display-1.pt-3 wizard])]))



;; ----------------------------------------------------------------------------
;; Setup

(defn player-data
  [i player-name]
  {:player-name player-name
   :index i
   :resources   (into {} (for [t resource-types] [t 1]))})

(rf/reg-event-db
  :game/setup
  (fn [db _]
    (rf/dispatch [:board/setup])
    (assoc db
      :players (into [] (map-indexed player-data ["cupid" "zeus" "hades"]))
      :current-player-name "cupid"
      :placing false)))

(rf/reg-sub
  :players
  (fn [db _]
    (:players db)))

(rf/reg-sub
  :current-player-name
  (fn [db _]
    (:current-player-name db)))

(rf/reg-sub
  :current-player-idx
  (fn [db _]
    (:index (get-only @(rf/subscribe [:players])
                      :player-name
                      @(rf/subscribe [:current-player-name])))))

(rf/reg-sub
  :next-player-name
  (fn [db _]
    (let [players @(rf/subscribe [:players])
          cur-idx @(rf/subscribe [:current-player-idx])
          next-idx (if (= (+ 1 cur-idx) (count players)) 0 (+ 1 cur-idx))]
      (:player-name (nth players next-idx)))))

;; ----------------------------------------------------------------------------
;; Placing Things

(rf/reg-event-db
  :place/start
  (fn [db [_ structure placer]]
    #p (assoc db :placing (get-only structures :type structure)
                 :placer  placer)))

(rf/reg-event-db
  :place/tile
  (fn [db [_ {:keys [row-idx col-idx]}]]
    (let [tile (get-in db [:board row-idx col-idx])]
      (-> db
          (update-in [:board row-idx col-idx :structures]
                     #(conj %
                            (assoc (:placing db)
                              :owner      (:placer db)
                              :production (get-in tile [:land :production]))))
          (assoc :placing false)))))

(rf/reg-sub
  :placing
  (fn [db _]
    (:placing db)))

;; ----------------------------------------------------------------------------
;; End of Turn

(rf/reg-event-db
  :end-turn
  (fn [db [_]]
    (let [current-player-name @(rf/subscribe [:current-player-name])
          current-player-idx  @(rf/subscribe [:current-player-idx])
          owned-structures    (filter #(= (:owner %) current-player-name)
                                @(rf/subscribe [:structures]))
          total-production    (if owned-structures
                                (apply merge-with
                                  +
                                  (map :production owned-structures))
                                (into {} (for [rt resource-types] [rt 0])))]
      #p (-> db
             (update-in [:players current-player-idx :resources]
                        #(merge-with + total-production %))
             (assoc :current-player-name @(rf/subscribe
                                            [:next-player-name]))))))


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
   {:fx [[:log/error (str "Could not query the experiments. Did you forget to start the api? " error)]]}))

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
  (.render root (r/as-element [main])))

(defn- ^:dev/after-load re-render
  "The `:dev/after-load` metadata causes this function to be called after
  shadow-cljs hot-reloads code. This function is called implicitly by its
  annotation."
  []
  (rf/clear-subscription-cache!)
  (init))
