(ns app.interface.developments
  (:require [re-frame.core :as rf]
            [app.interface.board :refer [lands update-tiles]]
            [app.interface.utils :refer [get-only]]))

(def all-land-types
  (into #{} (for [land lands]
              (:type land))))

(defn update-resources
  [db player-idx resource-delta]
  (let [updated (update-in db
                           [:players player-idx :resources]
                           #(merge-with + resource-delta %))]
    (if (seq (filter (fn [[_ amount]] (> 0 amount))
               (get-in updated [:players player-idx :resources])))
      ; NEGATIVE RESOURCE ERROR!
      nil
      updated)))
  
(def developments
  [{:type        :settlement
    :description "Produces resources"
    :legal-land-placements all-land-types
    :use (fn [db instance]
           (update-resources db @(rf/subscribe [:current-player-idx]) 
                             (:production instance)))
    :max 6
    :cost        {:wood -1}
    :tax         {:food -1}
    :production  {}}
   {:type        :library
    :description "Draw 3 cards, discard 2"
    :legal-land-placements #{"mountain"}
    :use (fn [db instance]
            (assoc db :message "No cards in game yet"))
    :max 2
    :cost        {:wood -1}
    :tax         {:sand -1}
    :production  {}}
   {:type        :terraformer
    :description "Change the land type of a tile"
    :legal-land-placements #{"sand"}
    :use (fn [db instance]
            (assoc db :message "Terraformer not implemented yet"))
    :max 3
    :cost        {:wood -1}
    :tax         {:stone -1}
    :production  {}}])

 
(rf/reg-event-db
  :development/use
  (fn [db [_ development {:keys [row-idx col-idx worker-owner]}]]
    (let [current-player-idx  @(rf/subscribe [:current-player-idx])
          current-player-name @(rf/subscribe [:current-player-name])]
      (if worker-owner
        (assoc db :message (str "Worker from " worker-owner
                                " already here!"))
        (-> db
            (assoc-in [:board row-idx col-idx :worker-owner]
                      (:current-player-name db))
            ; Pay tax if not your development
            (update-resources current-player-idx
                              (if (= current-player-name (:owner development))
                                {}
                                (:tax development)))
            ((:use development) development))))))
      

(rf/reg-event-db
  :development/start-placing
  (fn [db [_ development-type placer]]
    (let [development (get-only developments :type development-type)]
      (->
        db
        (assoc :board (update-tiles (:board db)
                                    (fn [tile]
                                      (assoc tile
                                        :legal-placement?
                                          (contains? (:legal-land-placements
                                                       development)
                                                     (:type (:land tile)))))))
        (assoc :placing development
               :placer  placer)))))

(rf/reg-event-db
  :development/place
  (fn [db [_ {:keys [row-idx col-idx legal-placement?]}]]
    (let [tile         (get-in db [:board row-idx col-idx])
          current-player-idx @(rf/subscribe [:current-player-idx])
          existing-num (count @(rf/subscribe [:developments
                                              (:type (:placing db))]))
          cost-paid-db (update-resources db
                                         current-player-idx
                                         (:cost (:placing db)))]
      (cond
        (not legal-placement?)
        (assoc db :message "Invalid location!")
        (>= existing-num (:max (:placing db)))
        (assoc db :message "Max number already placed!")
        (nil? cost-paid-db)
        (assoc db :message "Cannot pay the cost!")
        (= 0 (get-in db [:players current-player-idx :workers]))
        (assoc db :message "No more workers!")
        :else
        (-> cost-paid-db
            (assoc-in [:board row-idx col-idx :development]
                      (assoc (:placing db)
                        :worker-owner (:placer db)
                        :owner      (:placer db)
                        :production (get-in tile [:land :production])))
            (assoc-in [:board row-idx col-idx :worker-owner]
                      (:current-player-name db))
            (update-in [:players current-player-idx :workers] dec)
            (assoc :placing false))))))

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
