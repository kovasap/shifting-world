(ns app.interface.developments
  (:require
   [re-frame.core :as rf]
   [app.interface.players
    :refer
    [get-current-player update-resources update-resources-with-check]]
   [app.interface.board :refer [update-tiles adjacent-to-owned-developments?
                                get-adjacent-tiles]]
   [app.interface.utils :refer [get-only]]))

(defn is-legal-placement?-shared
  [db tile]
  (and (nil? (:development tile))
       (adjacent-to-owned-developments?
         (:board db) tile (get-current-player db))))


(defn has-resource-sources?
  [development board tile]
  (let [adjacent-developments (filter #(not (nil? %))
                                      (map :development
                                           (get-adjacent-tiles board tile)))
        inputs (reduce merge-with + (map :production adjacent-developments))]))
    ; TODO make sure that the sum of all possible inputs contains the needed
    ; inputs for this development


; TODO add malli spec for development

(defn accumulate-land-resources
  [{:keys [development] :as tile}]
  (if (and development (:land-accumulation development))
    (update tile :claimable-resources
      #(merge-with + % (:land-accumulation tile)))
    tile))


(defn get-neg-pairs [m] (into {} (for [[k v] m :when (neg? v)] [k v])))
  

(defn drain-resources
  "Returns the drained tile if successful, nil if the resources requested could
  not be drained from the given tile.
  Resources is a map of resources to quantities, with negative quantities to
  drain."
  [tile resources]
  (let [drained-resources (merge-with + (:claimable-resources tile)
                                     (get-neg-pairs resources))]
    (if (get-neg-pairs drained-resources)
      nil
      drained-resources)))


(defn execute-production-chain
  "Returns an updated board."
  [production-chain board tile]
  (let [adj-tiles (get-adjacent-tiles board tile)
        available-resources (reduce (partial merge-with +)
                                    (map :claimable-resources adj-tiles))]
    (if (drain-resources tile production-chain)
      ; This production-chain is possible
      ; Go through each tile and remove as many resources as possible until the
      ; chain's resources are satisfied.
      tile
      ; This chain is not possible
      board)))
      


(defn accumulate-production-resources
  "Remove consumed and add produced resources."
  [board {:keys [development] :as tile}]
  ((apply comp (for [pc (:production-chains development)]
                 #(execute-production-chain pc % tile)))
   board))
  
(def developments
  [{:type        :settlement
    :letter      "S"
    :description "Accumulates resources for future collection/processing."
    :is-legal-placement? (fn [db tile] (is-legal-placement?-shared db tile))
    :land-accumulation {:forest {:wood 1}
                        :plains {:food 1}
                        :water {:water 1}}
    :production-chains []
    :resource-accumulation (fn [tile]
                             (update tile :claimable-resources
                               #(merge-with + % (:production (:land tile)))))
    :max         6
    :cost        {:wood -1}
    :tax         {:food -1}
    :production  {}}
   {:type        :mill
    :letter      "M"
    :description "Produces planks from wood AND/OR flour from grain."
    :land-accumulation {}
    :production-chains [{:wood -1 :planks 1}
                        {:grain -1 :flour 1}]
    :is-legal-placement? (fn [db tile]
                           (and (= :plains (:type (:land tile)))
                             (is-legal-placement?-shared db tile)))
    :resource-accumulation (fn [tile]
                             (update tile :claimable-resources
                               #(merge-with + % (:production (:land tile)))))
    :max         6
    :cost        {:wood -1}
    :tax         {:food -1}
    :production  {}}
   {:type        :trading-post
    :description "Trade any resources 2 to 1."
    :is-legal-placement? (fn [db tile]
                           (and (= :plains (:type (:land tile)))
                             (is-legal-placement?-shared db tile)))
    :max         6
    :cost        {:wood -1}
    :tax         {:food -1}
    :production  {}}
   {:type        :capitol
    :description "Take starting player and get some resources"
    :is-legal-placement? (fn [db tile] (nil? (:development tile)))
    :use         (fn [db instance tile]
                   (let [current-player (get-current-player db)]
                     (-> db
                         (update :players
                                 (fn [ps]
                                   (into [current-player]
                                         (remove #(= % current-player) ps))))
                         (update-resources (:current-player-idx db)
                                           (:production instance)))))
    :max         3
    :cost        {:stone -5}
    :tax         {}
    :production  {:water 1}}
   {:type        :library
    :description "Draw 3 cards, discard 2"
    :is-legal-placement? (fn [db tile]
                           (and (= :mountain (:type (:land tile)))
                                (is-legal-placement?-shared db tile)))
    :use         (fn [db instance tile] (assoc db :message "No cards in game yet"))
    :max         2
    :cost        {:wood -1}
    :tax         {:sand -1}
    :production  {}}
   {:type        :terraformer
    :description "Change the land type of a tile"
    :is-legal-placement? (fn [db tile]
                           (and (= :sand (:type (:land tile)))
                                (is-legal-placement?-shared db tile)))
    :use         (fn [db instance tile]
                   (assoc db :message "Terraformer not implemented yet"))
    :max         3
    :cost        {:wood -1}
    :tax         {:stone -1}
    :production  {}}])


(defn claim-resources
  [db player-idx {:keys [claimable-resources row-idx col-idx]}]
  (-> db
    (update-resources player-idx claimable-resources)
    (update-in [:board row-idx col-idx] #(assoc % :claimable-resources {}))))

 
(rf/reg-event-db
  :development/use
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
                         (claim-resources (:current-player-idx db) tile)
                         (use-fn development tile))
        :else        updated-db))))

(defn stop-placing
  ([db]
   (-> db
       (assoc :board (update-tiles (:board db)
                                   (fn [tile]
                                     (assoc tile :legal-placement? false))))
       (assoc :placing false)))
  ([db _] (stop-placing db)))

(rf/reg-event-db
  :development/start-placing
  (fn [db [_ development-type placer]]
    (let [development (get-only developments :type development-type)]
      (-> db
          (stop-placing)
          (assoc :board (update-tiles (:board db)
                                      (fn [tile]
                                        (assoc tile
                                          :legal-placement?
                                            ((:is-legal-placement? development)
                                             db
                                             tile)))))
          (assoc :placing development
                 :placer  placer)))))

(rf/reg-event-db :development/stop-placing stop-placing)

(rf/reg-event-db
  :development/place
  (fn [db
       [_
        {:keys [row-idx col-idx legal-placement? claimable-resources] :as tile}]]
    (let [existing-num        (count @(rf/subscribe [:developments
                                                     (:type (:placing db))]))
          current-player-name (:player-name @(rf/subscribe [:current-player]))
          [cost-payable updated-db] (update-resources-with-check
                                      db
                                      (:current-player-idx db)
                                      (:cost (:placing db)))]
      (cond
        (not legal-placement?) (assoc db :message "Invalid location!")
        (>= existing-num (:max (:placing db)))
        (assoc db :message "Max number already placed!")
        (= 0 (get-in db [:players (:current-player-idx db) :workers]))
        (assoc db :message "No more workers!")
        (not cost-payable) updated-db
        :else
        (->
          updated-db
          (update-in
            [:board row-idx col-idx]
            #(assoc %
                :development  (assoc (:placing db)
                                :owner      (:placer db)
                                :production (get-in %
                                                    [:land :production]))
                :controller   (get-in db [:players (:current-player-idx db)])
                :worker-owner current-player-name))
          (update-in [:players (:current-player-idx db) :workers] dec)
          (claim-resources (:current-player-idx db) tile)
          (stop-placing))))))
        

(rf/reg-sub
  :placing
  (fn [db _]
    (:placing db)))


(rf/reg-sub
  :developments
  (fn [db [_ development-type]]
    (filter #(= (:type %) development-type)
      (reduce concat
        (for [column (:board db)]
          (for [tile column] (:development tile)))))))
