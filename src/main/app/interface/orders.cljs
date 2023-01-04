(ns app.interface.orders
  (:require [re-frame.core :as rf]))

(def orders
  [{:title "Claim land"
    :points 2
    :description "Own at least 10 tiles."}
   {:title "Lumberjack"
    :points 1
    :description "Turn in 10 planks."}
   {:title "Stonecutter"
    :points 1
    :description "Turn in 10 stone blocks."}
   {:title "Jeweler"
    :points 1
    :description "Turn in 1 perfect gem."}])
   
(rf/reg-sub
  :orders
  (fn [db _]
    (:orders db)))
