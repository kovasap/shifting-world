(ns app.interface.developments
  (:require [re-frame.core :as rf]
            [app.interface.board :refer [lands update-tiles]]
            [app.interface.utils :refer [get-only]]))

(def all-land-types
  (into #{} (for [land lands]
              (:type land))))

(def developments
  [{:type        :settlement
    :description "Produces resources"
    :legal-land-placements all-land-types
    :cost        {:wood -1}
    :tax         {:food -1}
    :production  {}}
   {:type        :library
    :description "Draw 3 cards, discard 2"
    :legal-land-placements #{"mountain"}
    :cost        {:wood -1}
    :tax         {:sand -1}
    :production  {}}
   {:type        :terraformer
    :description "Change the land type of a tile"
    :legal-land-placements #{"sand"}
    :cost        {:wood -1}
    :tax         {:stone -1}
    :production  {}}])


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
  
  
(rf/reg-event-db
  :development/use
  (fn [db [_ development]]
    (let [current-player-idx  @(rf/subscribe [:current-player-idx])
          current-player-name @(rf/subscribe [:current-player-name])]
      (-> db
          ; Pay tax if not your development
          (update-resources current-player-idx
                            (if (= current-player-name (:owner development))
                              {}
                              (:tax development)))))))
      

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
  (fn [db [_ {:keys [row-idx col-idx]}]]
    (let [tile         (get-in db [:board row-idx col-idx])
          current-player-idx @(rf/subscribe [:current-player-idx])
          cost-paid-db (update-resources db
                                         current-player-idx
                                         (:cost (:placing db)))]
      (if (nil? cost-paid-db)
        db
        (-> cost-paid-db
            (assoc-in [:board row-idx col-idx :development]
                      (assoc (:placing db)
                        :owner      (:placer db)
                        :production (get-in tile [:land :production])))
            (assoc :placing false))))))

(rf/reg-sub
  :placing
  (fn [db _]
    (:placing db)))


