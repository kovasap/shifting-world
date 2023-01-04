(ns app.interface.view.orders
  (:require [re-frame.core :as rf]))

(defn order-view
  [{:keys [title points description]}]
  [:div
   [:h3 title " (" points " pts)"]
   [:p description]])
