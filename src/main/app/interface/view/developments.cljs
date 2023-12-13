(ns app.interface.view.developments
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [clojure.string :as st]
            [app.interface.utils :refer [get-only]]
            [app.interface.developments :refer [developments resources]]
            ["cytoscape" :as cytoscape]))


(def dev-desc-hover-state (r/atom {}))
(defn development-desc-view
  [development-type row-idx col-idx]
  (let [unique-key [row-idx col-idx]
        development (get-only developments :type development-type)]
    [:div {:style         {:width    "100%"
                           :height   "100%"
                           :position "absolute"
                           :z-index  1}
           :on-mouse-over #(swap! dev-desc-hover-state (fn [state]
                                                         (assoc state
                                                           unique-key true)))
           :on-mouse-out  #(swap! dev-desc-hover-state (fn [state]
                                                         (assoc state
                                                           unique-key false)))}
     [:div
      {:style {:position   "absolute"
               :background "white"
               :overflow   "visible"
               :text-align "left"
               :top        50
               :z-index    2
               :display    (if (get @dev-desc-hover-state unique-key)
                             "block"
                             "none")}}
      (:description development)]]))


(def unique-id (atom 1))
(defn development-blueprint-view
  [development]
  (let [dev-name            (name (:type development))
        existing-num        @(rf/subscribe [:num-developments
                                            (:type development)])
        current-player-name (:player-name @(rf/subscribe [:current-player]))
        placing             @(rf/subscribe [:placing])
        placing-current     (= placing (:type development))]
    (swap! unique-id inc)
    [:div {:key      (str dev-name @unique-id) ; Required by react (otherwise
                                               ; we get a warning).
           :style    {:background  (if (:not-implemented development)
                                     "LightGrey"
                                     "LightBlue")
                      :text-align  "left"
                      :width       "250px"
                      :height      "300px"
                      :flex        1
                      :padding     "15px"
                      :font-weight (if placing-current "bold" "normal")
                      :border      "2px solid black"}
           :on-click (if (:not-implemented development)
                        #(prn "not implemented")
                        #(if placing-current
                                (rf/dispatch [:development/stop-placing])
                                (rf/dispatch [:development/start-placing
                                              (:type development)
                                              current-player-name])))}
     [:div [:strong dev-name] " " existing-num "/" (:max development)]
     [:div
      [:small
       "Place in "
       (st/join ", " (sort (map name (:valid-lands development))))]]
     [:div (:description development)]
     (if (:production-chains development)
       [:div
        "Chains: "
        (for [chain (:production-chains development)]
          [:div {:key (str chain @unique-id)}
           (str chain)])]
       nil)
     (if (:land-production development)
       [:div
         "Harvests: "
         (for [[land production] (:land-production development)]
           [:div {:key (str land @unique-id)}
            (str land " : " production)])]
       nil)]))


; Should take a form like
;   [{:data {:id "a"}}
;    {:data {:id "b"}}
;    {:data {:id "c"}}
;    {:data {:id "d"}}
;    {:data {:id "e"}}
;    {:data {:id "ab" :source "a" :target "b"}}
;    {:data {:id "ad" :source "a" :target "d"}}
;    {:data {:id "be" :source "b" :target "e"}}
;    {:data {:id "cb" :source "c" :target "b"}}
;    {:data {:id "de" :source "d" :target "e"}}))]
(defn make-development-graph
  [developments]
  (prn "devs" developments)
  (concat
    ; Development nodes
    (mapv (fn [{:keys [letter type]}] {:data {:id letter :label type}})
      developments)
    ; Edges
    (reduce concat
      (mapv (fn [{:keys [production-chains letter]}]
              (mapv (fn [[k v]]
                      {:data {:id     (str (name k) letter)
                              :source (if (> v 0) letter (name k))
                              :target (if (> v 0) (name k) letter)
                              :label  (str v)}})
                production-chains))
        developments))
    ; Resource Nodes
    (mapv (fn [resource]
            {:data {:id (name resource) :label (name resource)}})
      resources)))
  

(defn cytoscape-resource-flow
  "See inspiration at https://blog.klipse.tech/visualization/2021/02/16/graph-playground-cytoscape.html."
  [developments]
  (let [graph-element-id "graph"]
        ; developments @(rf/subscribe [:blueprints])]
    (r/create-class
      {:reagent-render      (fn [_] [:div
                                     "Cytoscape view:"
                                     [:div {:id    graph-element-id
                                            :style {:height "200px"
                                                    :width  "200px"}}]])
       ; We use this react lifecycle function because our graph-element-id div
       ; must exist before we call the cytoscape functionality that populates
       ; it.
       :component-did-mount (fn [_]
                              (cytoscape
                                (clj->js
                                  {:style     [{:selector "node"
                                                :style    {:background-color
                                                           "#666"
                                                           :label
                                                           "data(label)"}}
                                               {:selector "edge"
                                                :style    {:width 2
                                                           :line-color "#ccc"
                                                           :target-arrow-color
                                                           "#ccc"
                                                           :curve-style
                                                           "bezier"
                                                           :target-arrow-shape
                                                           "triangle"
                                                           :label
                                                           "data(label)"}}]
                                   :layout    {:name "circle"}
                                   :userZoomingEnabled false
                                   :userPanningEnabled false
                                   :boxSelectionEnabled false
                                   :container (js/document.getElementById
                                                graph-element-id)
                                   :elements  (make-development-graph
                                                developments)})))})))
       
       


(defn blueprints
  []
  ; TODO calculate this height based on the board height instead of hardcoding
  (let [developments (sort-by (fn [{:keys [not-implemented type]}]
                                [not-implemented type])
                              @(rf/subscribe [:blueprints]))]
    [:div {:style {:height "1000px" :width "600px" :overflow "auto"}}
     [:div
      "Take turns placing developments by clicking on the one you want to "
      "place then clicking on a valid (highlighted) location to place it. "
      "You can cancel by clicking on the development again. "
      "Click \"End Turn\" to advance to the next player. "]
     [:br]
     [:div
      "You get 2 points for each adjacent development you have to other "
      "players."]
     [:br]
     [:div
      "The game ends when all developments are placed "
      "(note the limit next to each development name)!"]
     [:br]
     [cytoscape-resource-flow developments]
     [:br]
     (into [:div {:style {:display       "grid"
                          :grid-template-columns "auto auto"
                          :margin-bottom "100%"
                          :grid-gap      "10px"}}]
           (for [development developments]
             (development-blueprint-view development)))]))
