(ns app.interface.orders
  (:require [re-frame.core :as rf]))

(def orders
  [{:title "Landlord"
    :points 2
    :description "Own at least 10 tiles."}
   {:title "Explorer"
    :points 2
    :description "Have a development on 4 different tile types."}
   {:title "Mayor"
    :points 2
    :description "Have 5 developments that all connect."}
   {:title "Sampler"
    :points 2
    :description "Turn in one of 7 different resources."}
   {:title "Common man"
    :points 2
    :description "Turn in 15 of the same basic resource."}
   {:title "Aristocrat"
    :points 2
    :description "Turn in 5 of the same luxury resource."}
   {:title "Artisan"
    :points 2
    :description "Turn in 5 of the same intermediate resource."}
   {:title "Jeweler"
    :points 1
    :description "Turn in 1 perfect gem."}])
   
(rf/reg-sub
  :orders
  (fn [db _]
    (:orders db)))
