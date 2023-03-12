(ns app.interface.developments
  (:require [app.interface.lands :refer [lands]]
            [clojure.set :refer [difference]]))

; TODO add malli spec for development

(def generally-valid-lands
  (difference (set (map :type lands)) #{:water :void}))

; TODO add:
;  - development that's just worth points
;  - railroad or crossroads that can connect production chain tiles
; These are references to development that are not to be copied, just
; referenced via their :type or :letter.  This is because they may contain
; functions, which cannot be serialized as game state.
(def developments
  [{:type        :settlement
    :letter      "S"
    :description "Accumulates resources for future collection/processing."
    :land-production {:forest {:wood 2}
                      :plains {:food 2}
                      :mountain {:stone 2}
                      :water {:water 2}}
    :on-placement (fn [db] db)
    :valid-lands generally-valid-lands
    :max         6}
   {:type        :hideout
    :letter      "H"
    :description "All adjacent settlements accumulate one less resource per
                 turn (bandits are stealing them)."
    :not-implemented true
    :on-placement (fn [db] db) ; TODO implement
    :valid-lands generally-valid-lands
    :max         6}
   {:type        :monument
    :letter      "T"
    :description "Worth 5 pts"
    :on-placement (fn [db]
                    (update-in db [:players (:current-player-idx db)]
                               #(update % :points (partial + 5))))
    :valid-lands generally-valid-lands
    :max         6}
   {:type        :mill
    :letter      "M"
    :description "Produces planks from wood AND/OR flour from grain."
    :on-placement (fn [db] db)
    :production-chains [{:wood -1 :planks 1}
                        {:grain -1 :flour 1}]
    :valid-lands generally-valid-lands
    :max         6}
   {:type        :oven
    :letter      "O"
    :description "Produces charcoal from wood AND/OR bread from flour"
    :land-accumulation {}
    :on-placement (fn [db] db)
    :production-chains [{:wood -1 :charcoal 1}
                        {:flour -1 :bread 1}]
    :valid-lands #{:plains}
    :max         6}
   {:type        :trading-post
    :description "Trade resources 2 to 1 according to what trades are available
                 from a rotating trading wheel of options (not yet implemented)."
    :on-placement (fn [db] db)
    :not-implemented true
    :valid-lands #{:plains}
    :max         6}
   #_{:type        :capitol
      :description "Take starting player and get 1 water. Worth 2 pts at the end
                 of the game."
      :is-legal-placement? (fn [db tile] (nil? (:development tile)))
      :use         (fn [db instance tile]
                     (let [current-player (get-current-player db)]
                       (-> db
                           (update :players
                                   (fn [ps]
                                     (into [current-player]
                                           (remove #(= % current-player) ps))))
                           (update-resources (:current-player-idx db)
                                             {:water 1}))))
      :max         3
      :cost        {:stone -5}
      :tax         {}}
   {:type        :library
    :description "Can use opponent development without paying them a VP"
    :valid-lands #{:mountain}
    :on-placement (fn [db] db)
    :not-implemented true
    :max         2}
   {:type        :terraformer
    :description "Change the land type of any tile"
    :not-implemented true
    :valid-lands generally-valid-lands
    :on-placement (fn [db] db)
    :max         3}])
