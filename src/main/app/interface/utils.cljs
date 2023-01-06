(ns app.interface.utils)

(defn only
  "Gives the sole element of a sequence"
  [x] {:pre [(nil? (next x))]} (first x))

(defn get-only
  [list-of-maps k v]
  (only (get (dissoc (group-by k list-of-maps) nil) v)))
