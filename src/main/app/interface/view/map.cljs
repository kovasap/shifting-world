(ns app.interface.view.map
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [clojure.string :as st]
            [app.interface.config :refer [debug]]
            [app.interface.utils :refer [get-only]]
            [app.interface.view.developments :refer [development-desc-view]]))


; See resources/public/css/board.css for supporting css.
; TODO make better when
; https://github.com/schnaq/cljs-re-frame-full-stack/issues/1 is fixed
(def tile-hover-state (r/atom {}))
(defn tile-view
  [{:keys [land
           row-idx
           col-idx
           development-type
           legal-placement-or-error
           production
           controller-name]
    :as   tile}]
  (let [adjacent-tiles   @(rf/subscribe [:adjacent-tiles tile])
        hovered          (get-in @tile-hover-state [row-idx col-idx])
        legal-placement? (not (string? legal-placement-or-error))
        controller       (get-only @(rf/subscribe [:players])
                                   :player-name
                                   controller-name)]
    [:div.tile
     {:style         {:font-size  "12px"
                      :text-align "center"
                      :position   "relative"}
      ; Run the placement animation.
      :class         (if development-type "activate" "")
      :on-mouse-over #(doseq [{t-row-idx :row-idx t-col-idx :col-idx}
                              (conj adjacent-tiles tile)]
                        (swap! tile-hover-state (fn [state]
                                                  (assoc-in state
                                                    [t-row-idx t-col-idx]
                                                    true))))
      :on-mouse-out  #(doseq [{t-row-idx :row-idx t-col-idx :col-idx}
                              (conj adjacent-tiles tile)]
                        (swap! tile-hover-state (fn [state]
                                                  (assoc-in state
                                                    [t-row-idx t-col-idx]
                                                    false))))
      :on-click      #(cond @(rf/subscribe [:placing]) (rf/dispatch
                                                         [:development/place
                                                          tile])
                            development-type (rf/dispatch [:development/use
                                                           development-type
                                                           tile])
                            :else (rf/dispatch [:message
                                                "Can't do anything here"]))}
     [:div.background
      {:style (merge (:style land)
                     {:width    "100%"
                      :height   "100%"
                      :position "absolute"
                      :z-index  -1
                      :opacity  (cond (and legal-placement? hovered) 1.0
                                      legal-placement? 0.8
                                      :else 0.7)})}]
     ; Note that the "clip-path" property that makes the hexagon shapes applies
     ; to all child divs, making it impossible for them to overflow their
     ; parent.
     (development-desc-view development-type row-idx col-idx)
     [:div {:style {:position "absolute" :padding-top "10px" :width "100%"}}
      [:div {:style {:display (if debug "block" "none")}}
       row-idx
       ", "
       col-idx]
      [:div {:style {:color (:color controller)}}
       (if controller-name (str controller-name "'s") nil)]
      [:div development-type]
      [:div (st/join ", " (for [[k v] production
                                :when (> v 0)]
                           (str v " " (name k))))]]]))


; Defined as --s and --m in resources/public/css/board.css.  These values must
; be kept in sync!
(def hex-tile-size-px 150)
(def hex-margin-px 5)
(defn required-hex-grid-px-width
  [board]
  (let [board-cols (count (first board))]
    (+ (* 2 hex-margin-px board-cols)
       ; add 1 to board-cols here to make sure that every row has the same
       ; number of hexes
       (* hex-tile-size-px (+ 1 board-cols)))))

(defn board-view
  []
  (let [board @(rf/subscribe [:board])]
    [:div.boardmain
      (into
        [:div.board
         {:style {:width (str (required-hex-grid-px-width board) "px")}}]
        (reduce concat
          (for [column board] (for [tile column] (tile-view tile)))))]))
