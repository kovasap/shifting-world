(ns app.interface.resource-flow
  (:require
    [app.interface.map-generation :refer [parse-board-str]]
    [app.interface.board :refer [get-adjacent-tiles]]))


(defn neg-pairs [m] (into {} (for [[k v] m :when (neg? v)] [k v])))
(defn pos-pairs [m] (into {} (for [[k v] m :when (pos? v)] [k v])))


(defn chain-active?
  [board tile production-chain])


(defn active-production-chains
  "A collection of all production chains that are active for a tile"
  [board {{:keys [production-chains]} :development :as tile}]
  (if (nil? production-chains)
    []
    (filter #(chain-active? board tile %) production-chains)))
  

(defn provided-resources
  "All resources this tile can provide to its neighbors (assuming nothing is
  consumed)."
  [board {:keys [development land] :as tile}]
  (merge-with +
              ; Resources from land accumulation (e.g. settlements)
              ((:type land)) (:land-accumulation development) 
              ; Resources from active production chains
              (pos-pairs (active-production-chains board tile))))

              


(defn available-resources
  "All resources this tile can provide to its neighbors, minus the resources
  the neighbors are consuming."
  [board tile])


; ------------------------------------------

(defn update-board-tile
  "Returns a new board with the first tile in the list updated."
  [board tiles update-fn]
  (if (empty? tiles)
    board
    (-> board
      (update-fn (first tiles))
      (update-board-tile (rest tiles) update-fn))))
  

(defn update-board-tiles
  "Feeds the updated board as an additional argument to each call of update-fn."
  [board update-fn]
  (update-board-tile board (reduce concat board) update-fn))

(def -debug-board1
  (parse-board-str
    "Fw1 FM
     F   W"))

(assert (= 4
           (count (reduce concat
                    (update-board-tiles
                      -debug-board1
                      (fn [board {:keys [row-idx col-idx]}]
                        (assoc-in board
                          [row-idx col-idx :claimable-resources :poop]
                          1)))))))

(defn accumulate-land-resources
  [{:keys [development] :as tile}]
  (if (and development (:land-accumulation development))
   (update tile :claimable-resources
    #(merge-with + % ((:type (:land tile)) (:land-accumulation development))))
   tile))



(defn resource-diff
  "Returns [claimable resources minus resources to drain,
            any remaining resources that need to be drained]."
  [claimable-resources resources-to-drain]
  (let [diff (merge-with +
                         claimable-resources
                         (neg-pairs resources-to-drain))]
    [(into {}
           (for [[resource amount] diff]
             [resource (if (neg? amount) 0 amount)]))
     (neg-pairs diff)]))


(assert (= [{:wood 0 :grain 1} {:wood -5}]
           (resource-diff {:wood 5 :grain 2} {:wood -10 :grain -1})))


(defn unmet-resources
  "Returns {:resource int} map of all resources that cannot be met for the
  given production chain."
  [production-chain board tile]
  (let [source-tiles (get-adjacent-tiles board tile)
        available-resources (apply merge-with
                              +
                              (for [{:keys [production]} source-tiles]
                                (pos-pairs production)))]
    (neg-pairs (merge-with + available-resources production-chain))))


(defn drain-resources
  [board candidate-tiles resources-to-drain]
  (cond
    (empty? (neg-pairs resources-to-drain))
    board  ; success!
    (empty? candidate-tiles)
    "No more tiles, draining impossible!"
    :else 
    (let [{:keys [row-idx col-idx production]} (first candidate-tiles)
          [remaining-claimable-resources remaining-resources-to-drain]
          (resource-diff production resources-to-drain)]
      (drain-resources (assoc-in board
                         [row-idx col-idx :production]
                         remaining-claimable-resources)
                       (rest candidate-tiles)
                       remaining-resources-to-drain))))

(defn apply-production-chain
  "Returns an updated board."
  [production-chain board {:keys [row-idx col-idx] :as tile}]
  (let [source-tiles (get-adjacent-tiles board tile)]
    (if (empty? (unmet-resources production-chain board tile))
      (update-in
        ; Remove production from surrounding tiles as needed.
        (drain-resources board source-tiles production-chain)
        [row-idx col-idx :production]
        ; Add the production from this chain to this tile.
        #(merge-with + (pos-pairs production-chain) %))
      ; This production-chain is impossible, so we do nothing
      board)))

(def -debug-board2
  (parse-board-str
    "FS  FM
     F   W"))

; These asserts are commented because they assume resources will be drained

; Impossible production chain
(assert (= -debug-board2
           (apply-production-chain
             {:wood -3} -debug-board2 (get-in -debug-board2 [0 1]))))

(let [updated-board (apply-production-chain {:wood -1 :planks 1}
                                            -debug-board2
                                            (get-in -debug-board2 [0 1]))]
  (assert (= 1 (get-in updated-board [0 0 :production :wood])))
  (assert (= 1 (get-in updated-board [0 1 :production :planks]))))
