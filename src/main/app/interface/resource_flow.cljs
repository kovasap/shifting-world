(ns app.interface.resource-flow
  (:require
    [app.interface.map-generation :refer [parse-board-str]]
    [app.interface.board
     :refer
     [update-tiles adjacent-to-owned-developments? get-adjacent-tiles]]))


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


(assert (= [{:wood 0 :grain 1} {:wood -5}]
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
    (if (empty? (get-neg-pairs resource-balance))
      ; This production-chain is possible
      ; Go through each tile and remove as many resources as possible until the
      ; chain's resources are satisfied.
      ; Also update the current tiles claimable resources.
      (assoc-in (drain-resources board source-tiles production-chain)
        [row-idx col-idx :claimable-resources]
        (merge-with + (get-pos-pairs production-chain) claimable-resources))
      ; This chain is not possible - it consumes more resources than are
      ; available!
      board)))

(def -debug-board2
  (parse-board-str
    "Fw1 FM
     F   W"))

(assert (= -debug-board2
           (execute-production-chain {:wood -2}
                                     -debug-board2
                                     (get-in -debug-board2 [0 1]))))

(assert (= 0
           (get-in (execute-production-chain {:wood -1}
                                             -debug-board2
                                             (get-in -debug-board2 [0 1]))
                   [0 0 :claimable-resources :wood])))

(defn accumulate-production-resources
  "Remove consumed and add produced resources."
  [board {{:keys [production-chains]} :development :as tile}]
  ((apply comp identity (for [pc production-chains]
                          #(execute-production-chain pc % tile)))
   board))

(assert (= 1
           (get-in (accumulate-production-resources -debug-board2
                                                    (get-in -debug-board2 [0 1]))
                   [0 1 :claimable-resources :planks])))
