(ns app.interface.development-placement
  (:require
   [re-frame.core :as rf]
   [day8.re-frame.undo :as undo :refer [undoable]] 
   [app.interface.players :refer [get-current-player]]
   [app.interface.developments :refer [developments]]
   [app.interface.lands :refer [lands]]
   [app.interface.board :refer [update-tiles adjacent-to-controlled-developments?]]
   [app.interface.resource-flow :refer [unmet-resources apply-production-chain]]
   [app.interface.utils :refer [get-only]]))


(defn available-resources
  [board player-name]
  (reduce (partial merge-with +)
    (for [column board]
      (for [{:keys [controller-name production-chains]} column
            :when (= controller-name player-name)]
        (into {} (for [[resource amount] production-chains
                       :when (> amount 0)]
                   [resource amount]))))))


(defn control-any-tiles?
  [board player-name]
  (> (count
      (reduce concat
        (for [column board]
          (for [{:keys [controller-name]} column
                :when (= controller-name player-name)]
            controller-name))))
     0))


(defn get-num-developments
  [board development-type]
  (count
    (filter #(= % development-type)
      (reduce concat
        (for [column board]
          (for [tile column] (:development-type tile)))))))

(rf/reg-sub
  :num-developments
  (fn [db [_ development-type]]
    (get-num-developments (:board db) development-type)))

(defn fail!
  [message]
  (rf/dispatch [:message message])
  false)


(defn is-legal-placement?
  "Returns a boolean"
  [development-type db tile]
  (let [development         (get-only developments :type development-type)
        valid-lands         (get development
                                 :valid-lands
                                 (set (map :type lands)))
        chains-to-unmet-resources
        (into {}
              (for [chain (:production-chains development)]
                [chain (unmet-resources chain (:board db) tile)]))
        current-player-name (:player-name (get-current-player db))]
    (cond
      (not (nil? (:development-type tile)))
      "Tile is already occupied!"
      (and (seq chains-to-unmet-resources)
           (every? seq (vals chains-to-unmet-resources)))
      (str "All possible production chains invalid! "
           chains-to-unmet-resources)
      (not (contains? valid-lands (:type (:land tile))))
      (str "Invalid land type, must be one of " valid-lands)
      (and (control-any-tiles? (:board db) current-player-name)
           (not
             (adjacent-to-controlled-developments? (:board db)
                                                   tile
                                                   (get-current-player db))))
      "Must adjacent to developments you own, unless you own no tiles!"
      (>= (get-num-developments (:board db) development-type)
          (:max development))
      "Max number already placed!"
      :else true)))


(every? identity nil)


#_(rf/reg-event-db
    :development/use
    (undoable "Development Use")
    (fn [db [_ development {:keys [row-idx col-idx worker-owner] :as tile}]]
      (let [current-player-name (:player-name @(rf/subscribe [:current-player]))
            tax (if (= current-player-name (:owner development))
                  {}
                  (:tax development))
            [cost-payable updated-db] (update-resources-with-check
                                        db (:current-player-idx db) tax)
            use-fn (if (:use development) (:use development) identity)]
        (cond
          worker-owner (assoc db
                         :message (str "Worker from "
                                       worker-owner
                                       " already here!"))
          cost-payable (-> updated-db
                           (update-in [:players
                                       (:current-player-idx db)
                                       :workers]
                                      dec)
                           (assoc-in [:board row-idx col-idx :worker-owner]
                                     current-player-name)
                           (use-fn development tile))
          :else        updated-db))))

(defn stop-placing
  ([db]
   (-> db
       (assoc :board (update-tiles (:board db)
                                   (fn [tile]
                                     (assoc tile
                                       :legal-placement-or-error
                                       "Not placing anything"))))
       (assoc :placing false)))
  ([db _] (stop-placing db)))

(rf/reg-event-db
  :development/start-placing
  (undoable "Development Placement Start")
  (fn [db [_ development-type placer]]
    (-> db
        (stop-placing)
        (assoc :board (update-tiles (:board db)
                                    (fn [tile]
                                      (assoc tile
                                        :legal-placement-or-error
                                        (is-legal-placement? development-type
                                                             db
                                                             tile)))))
        (assoc :placing development-type
               :placer  placer))))

(rf/reg-event-db :development/stop-placing stop-placing)


(defn update-board-with-development
  [development
   board
   {:keys [row-idx col-idx land] :as tile}
   current-player-name]
  (let [update-all-production (apply comp
                                (for [chain (:production-chains development)]
                                  #(apply-production-chain chain % tile)))]
    (-> board
        (update-in
          [row-idx col-idx]
          #(assoc %
             :development-type (:type development)
             :production       ((:type land) (:land-production development))
             :controller-name  current-player-name))
        (update-all-production))))

(defn get-resources-by-player
  [db player-to-exclude]
  (into {}
        (for [player-name (:players db)
              :when (not (= player-name player-to-exclude))]
          [player-name (available-resources (:board db) player-name)])))

(defn resources-by-player-diff
  [pre post]
  (for [player-name (keys pre)]
    (merge-with - (get pre player-name) (get post player-name))))

; TODO update player points based on this
; TODO make a wrapper function that adds these points to keep the code
; contained.
(defn points-for-sharing
  [resource-diff]
  (apply + (vals resource-diff)))
  

(rf/reg-event-db
  :development/place
  (undoable "Development Placement")
  (fn [db [_ {:keys [legal-placement-or-error] :as tile}]]
    (let [current-player-name (:player-name (get-current-player db))
          development         (get-only developments :type (:placing db))
          pre-placement-player-resources (get-resources-by-player
                                           db current-player-name)
          updated-db          (cond (string? legal-placement-or-error)
                                    (assoc db
                                      :message legal-placement-or-error)
                                    :else (-> db
                                              (update
                                                :board
                                                #(update-board-with-development
                                                   development
                                                   %
                                                   tile
                                                   current-player-name))
                                              ((:on-placement development))
                                              (stop-placing)))
          post-placement-player-resources (get-resources-by-player
                                            db current-player-name)
          other-player-resources-used (resources-by-player-diff 
                                        pre-placement-player-resources
                                        post-placement-player-resources)]
      updated-db)))
        

(rf/reg-sub
  :placing
  (fn [db _]
    (:placing db)))
