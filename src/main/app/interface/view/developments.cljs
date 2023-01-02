(ns app.interface.view.developments
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [app.interface.developments :refer [developments]]))


(defn development-card-view
  [development]
  ; TODO add a wrapping div that on hover triggers the showing of the card
  [:div {:style}
    [:div {:style {:position "fixed" :background "white"}}
       (:description development)]])
