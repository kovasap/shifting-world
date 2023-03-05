(ns app.interface.core
  (:require ["react-dom/client" :refer [createRoot]]
            [ajax.core :as ajax]
            [app.config :as config]
            [day8.re-frame.http-fx]
            [day8.re-frame.undo :as undo :refer [undoable]]  
            [goog.dom :as gdom]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [app.interface.sente :refer [send-game-state-to-server!]]
            [app.interface.players :refer [next-player-idx reset-workers]]
            [clojure.string :as st]
            [app.interface.view.main :refer [main]]
            [app.interface.board :refer [update-tiles]]
            [app.interface.map-generation :refer [setup-board]]
            [app.interface.developments :refer [developments]]
            [app.interface.development-placement]
            [app.interface.config :refer [debug]]
            [app.interface.resources :refer [resources]]
            [app.interface.orders :refer [orders]]
            [app.interface.utils :refer [get-only]]
            [cljs.pprint]
            [taoensso.timbre :as log]))

(rf/reg-sub
  :db-no-board
  (fn [db _]
    (dissoc db :board)))


;; ----------------------------------------------------------------------------
;; Setup

(def opening-hand-size 4)

(defn make-opening-hand
  []
  (conj (take opening-hand-size
              (shuffle (filter #(not (:not-implemented %)) developments)))
        (get-only developments :type :settlement)))
        
(defn player-data
  [i player-name]
  {:player-name     player-name
   :index           i
   :color           (get ["blue" "red" "purple" "black"] i)
   :workers         2
   :max-workers     2
   :blueprints      (make-opening-hand)
   :owned-resources (if debug
                      {:wood 2}
                      (into {}
                            (for [{:keys [type]} resources] [type 0])))})

(rf/reg-event-db
  :game/setup
  (fn [db _]
    (-> db
     (setup-board)
     (assoc
         :message ""
         :orders (take 3 (shuffle orders))
         :players (into [] (map-indexed player-data ["cupid" "zeus" "hades"]))
         :current-player-idx 0
         :placing false))))

(rf/reg-event-db
  :message
  (fn [db [_ message]]
    (assoc db :message message)))

(rf/reg-sub
  :message
  (fn [db _]
    (:message db)))

;; ----------------------------------------------------------------------------
;; End of Turn

(rf/reg-event-db
  :end-turn
  (undoable "Turn End")
  (fn [db [_]]
    (let [new-db (-> db
                   (assoc :current-player-idx (next-player-idx db)))]
      (send-game-state-to-server! new-db)
      new-db)))

(rf/reg-event-db
  :end-round
  (undoable "Round End")
  (fn [db [_]]
    (-> db
        ; TODO check if orders have been fulfilled and end the game if so.
        ; Give everyone their workers back.
        (update :players #(mapv reset-workers %))
        (update :board update-tiles #(assoc % :worker-owner nil))
        #_(update :board update-tiles accumulate-land-resources)
        #_(update :board update-board-tiles accumulate-production-resources))))


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
