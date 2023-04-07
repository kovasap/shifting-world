(ns app.interface.developments
  (:require [app.interface.lands :refer [lands]]
            [clojure.set :refer [difference]]))

(defn assoc-if-nil
  "Add k=v to the m if k is not already present in m."
  [m k v]
  (if (nil? (k m))
    (assoc m k v)
    m))
           

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
  (for
    [dev
     [{:type        :settlement
       :letter      "S"
       :description "Accumulates resources for future collection/processing."
       :land-production {:forest   {:wood 2}
                         :plains   {:food 2}
                         :mountain {:stone 2}
                         :water    {:water 2}}
       :max         6}
      {:type :hideout
       :letter "H"
       :description
       "All adjacent settlements accumulate one less resource per
                            turn (bandits are stealing them)."
       :not-implemented true
       :on-placement (fn [db] db) ; TODO implement
       :max 6}
      {:type         :carpenter
       :letter       "C"
       :description  "Transforms planks into points"
       :production-chains [{:planks -1 :points 3}]
       :max          2}
      {:type         :monument
       :letter       "T"
       :description  "Worth 5 pts"
       :points       5
       :production-chains [{:stone -4}]
       :max          6}
      {:type         :nature-preserve
       :letter       "N"
       :description  "Worth 5 pts"
       :points       5
       :production-chains [{:water -2 :stone 1}]
       :max          6}
      {:type        :mill
       :letter      "M"
       :description "Produces planks from wood AND/OR flour from grain."
       :production-chains [{:wood -1 :planks 1} {:grain -1 :flour 1}]
       :max         6}
      {:type        :oven
       :letter      "O"
       :description "Produces charcoal from wood AND/OR bread from flour"
       :production-chains [{:wood -1 :charcoal 1} {:flour -1 :bread 1}]
       :valid-lands #{:plains}
       :max         6}
      {:type :trading-post
       :description
       "Trade resources 2 to 1 according to what trades are available
                            from a rotating trading wheel of options (not yet implemented)."
       :not-implemented true
       :valid-lands #{:plains}
       :max 6}
      #_{:type :capitol
         :description
         "Take starting player and get 1 water. Worth 2 pts at the end
                            of the game."
         :is-legal-placement? (fn [db tile] (nil? (:development tile)))
         :use (fn [db instance tile]
                (let [current-player (get-current-player db)]
                  (-> db
                      (update :players
                              (fn [ps]
                                (into [current-player]
                                      (remove #(= % current-player) ps))))
                      (update-resources (:current-player-idx db) {:water 1}))))
         :max 3
         :cost {:stone -5}
         :tax {}}
      {:type        :library
       :description "Can use opponent development without paying them a VP"
       :valid-lands #{:mountain}
       :not-implemented true
       :max         2}
      {:type        :terraformer
       :description "Change the land type of any tile"
       :not-implemented true
       :max         3}]]
    (-> dev
        (assoc-if-nil :valid-lands generally-valid-lands)
        (assoc-if-nil :on-placement identity)
        (assoc-if-nil :land-accumulation {}))))
