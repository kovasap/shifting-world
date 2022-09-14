(ns app.interface.data-table
  (:require [clojure.string :as s :refer [capitalize]]
            [app.interface.csv :refer [download-as-csv]]
            [reagent-data-table.core :as rdt]))


(def sample-data
  [{:id 1 :name "Alice" :age 23 :info {:favourite-colour "Green" :pet "Cat"}}
   {:id 2 :name "Bob" :age 28 :info {:favourite-colour "Blue" :pet "Dog"}}
   {:id 3 :name "Charlie" :age 32 :info {:favourite-colour "White" :pet "rabbit"}}
   {:id 4 :name "David" :age 41 :info {:favourite-colour "Pink"}}
   {:id 5 :name "Everlasting Ermine"}])
   

; TODO pass this into maps->data-table instead of having this global var.
(def selected-rows (atom []))

(defn maps->data-table
  [maps]
  (reset! selected-rows maps)
  (let [filterable-columns (keys (first maps))]
    [rdt/data-table
     {:table-id "snazzy-table"
      :sf-input-id "search-field"
      ; :headers               [[:id "ID"] [:name "Name"] [:age "Age"]]
      :headers (for [k (keys (first maps))]
                 [k (capitalize (name k))])
      :rows maps
      :td-render-fn
        (fn [row col-id]
          (cond (= :name col-id)
                [:td
                 [:a {:href (str "http://127.0.0.1:8000/?currentDir="
                                 (:id row))}
                  (get row col-id)]]
                (= :wormlist col-id)
                [:button {:on-click #(download-as-csv (:wormlist row)
                                       "wormlist.csv")}
                 "Download as CSV"]
                :else
                (if (empty? (str (get row col-id)))
                  [:td {:style {:background :gold
                                :display    :block}}
                   "~~unknowable~~"]
                  (get row col-id))))
      :filterable-columns filterable-columns
      :filter-label "Search:"
      :filter-string ""
      ; :child-row-opts        (-> @app :table-data :child-rows)
      :sortable-columns (keys (first maps))
      :sort-columns [[:age true]]
      :table-state-change-fn ; #(prn %)}]))
        (fn [{:keys [filter-string]}]
          (reset! selected-rows
            (filter #(rdt/filter-row filter-string filterable-columns %)
                    maps)))}]))
