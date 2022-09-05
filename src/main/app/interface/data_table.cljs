(ns app.interface.data-table
  (:require [clojure.string :refer [capitalize]]
            [reagent-data-table.core :as rdt]))
            


(def sample-data
  [{:id 1 :name "Alice" :age 23 :info {:favourite-colour "Green" :pet "Cat"}}
   {:id 2 :name "Bob" :age 28 :info {:favourite-colour "Blue" :pet "Dog"}}
   {:id 3 :name "Charlie" :age 32 :info {:favourite-colour "White" :pet "rabbit"}}
   {:id 4 :name "David" :age 41 :info {:favourite-colour "Pink"}}
   {:id 5 :name "Everlasting Ermine"}])
   

(defn maps->data-table
  [maps]
  (prn maps)
  [rdt/data-table
   {:table-id              "snazzy-table"
    :sf-input-id           "search-field"
    ; :headers               [[:id "ID"] [:name "Name"] [:age "Age"]]
    :headers               (for [k (keys (first maps))]
                             [k (capitalize (name k))])
    :rows                  maps
    :td-render-fn
    (fn [row col-id]
      (cond
        (= :name col-id)
        [:td [:a {:href (str "http://127.0.0.1:8000/?currentDir="
                             (:id row))}
              (get row col-id)]]
        :else
        (if (empty? (str (get row col-id)))
          [:td {:style {:background :gold :display :block}} "~~unknowable~~"]
          (get row col-id))))
    :filterable-columns    (keys (first maps))
    :filter-label          "Search:"
    :filter-string         ""
    ; :child-row-opts        (-> @app :table-data :child-rows)
    :sortable-columns      (keys (first maps))
    :sort-columns          [[:age true]]
    :table-state-change-fn #(.log js/console %)}])
