(ns app.interface.csv
  (:require
    [clojure.string :refer [join]]))


(defn maps-to-csv
  [maps]
  (let [headers (keys (first maps))]
    (join "\n" (into [(join "," (mapv name headers))]
                     (for [m maps]
                       (join "," (for [h headers] (str (h m)))))))))


(defn download-as-csv
  [maps export-name]
  (let [data-blob (js/Blob. #js [(maps-to-csv maps)]
                            #js {:type "text/csv;charset=utf-8;"})
        link      (.createElement js/document "a")]
    (set! (.-href link) (.createObjectURL js/URL data-blob))
    (.setAttribute link "download" export-name)
    (.appendChild (.-body js/document) link)
    (.click link)
    (.removeChild (.-body js/document) link)))
