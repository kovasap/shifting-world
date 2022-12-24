(ns app.interface.core
  (:require ["react-dom/client" :refer [createRoot]]
            [ajax.core :as ajax]
            [app.config :as config]
            [day8.re-frame.http-fx]
            [goog.dom :as gdom]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [clojure.string :as st]
            [taoensso.timbre :as log]))


; See resources/public/css/board.css for supporting css.
; TODO make better when
; https://github.com/schnaq/cljs-re-frame-full-stack/issues/1 is fixed
(def tile-hover-state (r/atom {}))
(defn render-tile
  [{:keys [land row-idx col-idx structures] :as tile}]
  (let [placing @(rf/subscribe [:placing])
        hovered (get-in @tile-hover-state [row-idx col-idx])]
    [:div.tile {:class         land
                :style         {:opacity (cond (and placing hovered) 1.0
                                               placing 0.8
                                               :else   0.7)}
                :on-mouse-over #(swap! tile-hover-state (fn [state]
                                                          (assoc-in state
                                                            [row-idx col-idx]
                                                            true)))
                :on-mouse-out  #(swap! tile-hover-state (fn [state]
                                                          (assoc-in state
                                                            [row-idx col-idx]
                                                            false)))
                :on-click      #(if placing
                                  (rf/dispatch [:place/tile tile])
                                  nil)}
     (str structures)]))
(defn render-tiles
  [tiles]
  (into [:div.board
         ; https://www.w3schools.com/css/css_grid.asp
         {:style {:grid-template-columns (st/join " "
                                                  (repeat (count (first tiles))
                                                          "max-content"))}}]
        (reduce concat
          (for [column tiles] (for [tile column] (render-tile tile))))))

(defn- main
  "Main view for the application."
  []
  (let [experiments @(rf/subscribe [:experiments])
        board       @(rf/subscribe [:board])]
    [:div.container
     [:h1 "Welcome"]
     [:p "My first page!"]
     [:button.btn.btn-outline-primary {:on-click #(rf/dispatch [:board/setup])}
      "Setup Board"]
     [:br]
     [:br]
     (when board (render-tiles board))
     [:br]
     [:br]
     [:button.btn.btn-outline-primary {:on-click #(rf/dispatch
                                                    [:place/start :settlement])}
      "Place Settlement"]]))
     ; (when wizard [:p.display-1.pt-3 wizard])]))



;; ----------------------------------------------------------------------------
;; Board Setup

(def board-str
  "F F F F F F F F
   F F F W F F F F
   F F F W W F F F
   F F F F W F F F
   S S S S S F F F")

(defn parse-board-str
  [board-str]
  (into []
        (map-indexed (fn [row-idx line]
                       (into []
                             (map-indexed (fn [col-idx tile-letter]
                                            {:structures []
                                             :row-idx    row-idx
                                             :col-idx    col-idx
                                             :land       (case tile-letter
                                                           "F" "forest"
                                                           "W" "water"
                                                           "S" "sand"
                                                           :else "void")})
                                          (st/split (st/trim line) #" "))))
                     (st/split-lines board-str))))

(rf/reg-event-db
  :board/setup
  (fn [db _]
    (assoc db :board (parse-board-str board-str)
              :placing false)))

(rf/reg-sub
  :board
  (fn [db _]
    (:board db)))


;; ----------------------------------------------------------------------------
;; Placing Things

(rf/reg-event-db
  :place/start
  (fn [db [_ structure]]
    (assoc db :placing structure)))

(rf/reg-event-db
  :place/tile
  (fn [db [_ {:keys [row-idx col-idx]}]]
    (-> db
      (update-in [:board row-idx col-idx :structures] #(conj % (:placing db)))
      (assoc :placing false))))

(rf/reg-sub
  :placing
  (fn [db _]
    (:placing db)))


;; -----------------------------------------------------------------------------
;; Events and Subscriptions to query the backend and store the result in the
;; app-state.

(rf/reg-event-fx
 :experiments/get
 (fn [_ _]
   {:fx [[:http-xhrio {:method :get
                       :uri (str config/api-location "/experiments")
                       :format (ajax/transit-request-format)
                       :response-format (ajax/transit-response-format)
                       :on-success [:experiments.get/success]
                       :on-failure [:experiments.get/error]}]]}))

(rf/reg-event-db
 :experiments.get/success
 (fn [db [_ response]]
   (assoc db :experiments (:experiments response))))

(rf/reg-event-fx
 :experiments.get/error
 (fn [_ [_ error]]
   {:fx [[:log/error (str "Could not query the experiments. Did you forget to start the api? " error)]]}))

(rf/reg-sub
 :experiments
 (fn [db _]
   (:experiments db)))



(rf/reg-fx
 :log/error
 (fn [message]
   (log/error message)))



;; -- Entry Point -------------------------------------------------------------

(defonce root (createRoot (gdom/getElement "app")))

(defn init
  []
  (.render root (r/as-element [main])))

(defn- ^:dev/after-load re-render
  "The `:dev/after-load` metadata causes this function to be called after
  shadow-cljs hot-reloads code. This function is called implicitly by its
  annotation."
  []
  (rf/clear-subscription-cache!)
  (init))
