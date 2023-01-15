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



(defn accumulate-land-resources
  [{:keys [development] :as tile}]
  (if (and development (:land-accumulation development))
    (update tile :claimable-resources
      #(merge-with + % (:land-accumulation tile)))
    tile))


(defn get-neg-pairs [m] (into {} (for [[k v] m :when (neg? v)] [k v])))
(defn get-pos-pairs [m] (into {} (for [[k v] m :when (pos? v)] [k v])))
  

(defn resource-diff
  "Returns [claimable resources minus resources to drain,
            any remaining resources that need to be drained]."
  [claimable-resources resources-to-drain]
  (let [diff (merge-with +
                         claimable-resources
                         (get-neg-pairs resources-to-drain))]
    [(into {}
           (for [[resource amount] diff]
             [resource (if (neg? amount) 0 amount)]))
     (get-neg-pairs diff)]))


(assert (= [{:wood 0, :grain 1} {:wood -5, :grain 1}]
          (resource-diff {:wood 5 :grain 2} {:wood -10 :grain -1})))


(defn drain-resources
  [board candidate-tiles resources-to-drain]
  (if (or (empty? candidate-tiles) (empty? (get-neg-pairs resources-to-drain)))
    board
    (let [{:keys [row-idx col-idx claimable-resources]} (first candidate-tiles)
          [remaining-claimable-resources remaining-resources-to-drain]
          (resource-diff claimable-resources resources-to-drain)]
      (drain-resources (assoc-in board
                         [row-idx col-idx :claimable-resources]
                         remaining-claimable-resources)
                       (rest candidate-tiles)
                       remaining-resources-to-drain))))
              
(defn execute-production-chain
  "Returns an updated board."
  [production-chain
   board
   {:keys [row-idx col-idx claimable-resources] :as tile}]
  ; The current tile is added to the front of this list.
  (let [source-tiles     (conj (get-adjacent-tiles board tile) tile)
        resource-balance (reduce (partial merge-with +)
                           (conj (map :claimable-resources source-tiles)
                                 production-chain))]
    (if (get-neg-pairs resource-balance)
      ; This chain is not possible - it consumes more resources than are
      ; available!
      board
      ; This production-chain is possible
      ; Go through each tile and remove as many resources as possible until the
      ; chain's resources are satisfied.
      ; Also update the current tiles claimable resources.
      (drain-resources
        (assoc-in board [row-idx col-idx :claimable-resources]
          (merge-with + (get-pos-pairs production-chain) claimable-resources))
        source-tiles
        production-chain))))

(defn accumulate-production-resources
  "Remove consumed and add produced resources."
  [board {:keys [development] :as tile}]
  ((apply comp (for [pc (:production-chains development)]
                 #(execute-production-chain pc % tile)))
   board))


; TODO add malli spec for development
  
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
