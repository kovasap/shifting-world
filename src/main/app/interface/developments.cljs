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

(def basic-resources
  [:wood :grain :stone :water :fossils])

(def refined-resources
  [:planks :bread :flour :charcoal])

; TODO add:
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
                         :plains   {:grain 2}
                         :mountain {:stone 2}
                         :desert   {:fossils 1}
                         :water    {:water 2}}
       :max         12}
      ; ----------------- Resource Generators -------------------
      {:type        :mill
       :letter      "M"
       :description "Produces planks from wood AND/OR flour from grain."
       :production-chains [{:wood -1 :planks 1} {:grain -1 :flour 1}]
       :max         6}
      {:type        :oven
       :letter      "O"
       :description "Produces charcoal from wood AND/OR bread from flour"
       :production-chains [{:wood -1 :charcoal 1} {:flour -1 :bread 4}]
       :valid-lands #{:plains}
       :max         6}
      ; ----------------- Point Generators --------------------
      {:type        :monument
       :letter      "T"
       :description "Worth 5 pts"
       :production-chains [{:stone -6 :points 5}]
       :max         2}
      {:type        :nature-preserve
       :letter      "N"
       :description "Worth 5 pts"
       :production-chains [{:water -2 :points 5}]
       :max         2}
      {:type        :carpenter
       :letter      "C"
       :description "Transforms planks into points"
       :production-chains [{:planks -1 :points 3}]
       :max         2}
      {:type        :crossroads
       :letter      "X"
       :description "Worth 1 points for each adjacent development."
       :not-implemented true
       :max         2}
      {:type        :oasis
       :letter      "A"
       :description "Makes water and points"
       :production-chains [{:water 1 :points 1}]
       :max         2}
      {:type :throne
       :letter "E"
       :description
       "Worth 10 pts if you have the most tiles of at least 3 land types"
       :production-chains [{:stone -2}]
       :max 2}
      ; TODO make this an infinite sink?
      {:type        :port
       :letter      "P"
       :description "Resources to points"
       :production-chains (into []
                                (for [resource refined-resources]
                                  {resource -2 :points 2}))
       :valid-lands #{:water}
       :max         2}
      {:type        :house
       :letter      "H"
       :description "Bread to points"
       :production-chains [{:bread -1 :points 2}]
       :max         6}
      ; ----------------- Point Eaters ---------------------------------------
      {:type        :bandit-hideout
       :letter      "H"
       :description "Turns points into bread"
       :production-chains [{:points -2 :bread 1}]
       :max         6}
      ; ----------------- Misc ---------------------------------------
      {:type        :road
       :letter      "R"
       :description "Does nothing, but extends your buildable area"
       :production-chains [{:wood -1} {:stone -1}]
       :max         12}
      {:type        :marketplace
       :letter      "K"
       :description "Moves all resources from adjacent tiles to itself."
       :not-implemented true
       :max         4}
      {:type :trading-post
       :description
       "Trade resources 2 to 1 according to what trades are available
                            from a rotating trading wheel of options (not yet implemented)."
       :not-implemented true
       :valid-lands #{:plains}
       :max 6}
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
