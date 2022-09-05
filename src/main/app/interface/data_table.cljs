(ns app.interface.data-table
  (:require [reagent-data-table.core :as rdt]))


(def sample-data
  [{:id 1 :name "Alice" :age 23 :info {:favourite-colour "Green" :pet "Cat"}}
   {:id 2 :name "Bob" :age 28 :info {:favourite-colour "Blue" :pet "Dog"}}
   {:id 3 :name "Charlie" :age 32 :info {:favourite-colour "White" :pet "rabbit"}}
   {:id 4 :name "David" :age 41 :info {:favourite-colour "Pink"}}
   {:id 5 :name "Everlasting Ermine"}])
   

(defn maps->data-table
  [maps]
  [rdt/data-table
   {:table-id              "snazzy-table"
    :sf-input-id           "search-field"
    :headers               [[:id "ID"] [:name "Name"] [:age "Age"]]
    :rows                  maps
    :td-render-fn
    (fn [row col-id]
      (cond
        (and (= :name col-id)
             (even? (:id row))) [:td [:a {:href (str "http://example.com/pople/"
                                                     (:name row))}
                                      (get row col-id)]]
        :else (if (empty? (str (get row col-id)))
                [:td {:style {:background :gold :display :block}} "~~unknowable~~"]
                (get row col-id))))
    :filterable-columns    [:age :name]
    :filter-label          "Search by age or name:"
    :filter-string         "a"
    ; :child-row-opts        (-> @app :table-data :child-rows)
    :sortable-columns      [:id :name :age]
    :sort-columns          [[:age true]]
    :table-state-change-fn #(.log js/console %)}])
