(ns app.interface.sente
  (:require [cljs.core.async :as async :refer (<! >! put! chan)]
            [app.config :as config]
            [clojure.walk :refer [prewalk]]
            [re-frame.core :as rf]
            [taoensso.encore :as encore :refer-macros (have have?)]
            [taoensso.sente :as sente :refer (cb-success?)]))

(def ?csrf-token
  (when-let [el (.getElementById js/document "sente-csrf-token")]
    (.getAttribute el "data-csrf-token")))


(let [{:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket-client!
       "/chsk" ; Note the same path as before
       ?csrf-token
       {:type :auto
        :packer :edn
        :protocol :http
        :host "localhost"
        :port config/api-port})] ; e/o #{:auto :ajax :ws}
  (def chsk       chsk)
  (def ch-chsk    ch-recv) ; ChannelSocket's receive channel
  (def chsk-send! send-fn) ; ChannelSocket's send API fn
  (def chsk-state state))   ; Watchable, read-only atom



; Syncing Game State

(defn remove-fns
  "Useful for removing functions from the db before sending to the server.
  
  Currently only removes functions if they are values in a map, not if they are
  elements of a vector/list."
  [data]
  (prewalk (fn [node] (if (map? node)
                        (into {} (for [[k v] node
                                       :when (not (fn? v))]
                                   [k v]))
                        node))
           data))

(defn send-game-state-to-server!
  [db]
  (chsk-send! [:game/sync-state {:db (remove-fns db)}] 5000
              (fn [cb-reply] (prn "sent state to server"))))



(rf/reg-event-db
  :game/update-state
  (fn [_ [_ db-from-server]]
     db-from-server))

;;;; Sente event handlers

(defmulti -event-msg-handler
  "Multimethod to handle Sente `event-msg`s"
  :id) ; Dispatch on event-id
  

(defn event-msg-handler
  "Wraps `-event-msg-handler` with logging, error catching, etc."
  [{:as ev-msg :keys [id ?data event]}]
  (-event-msg-handler ev-msg))

(defmethod -event-msg-handler
  :default ; Default/fallback case (no other matching handler)
  [{:as ev-msg :keys [event]}]
  (prn "Unhandled event: %s" event))

(defmethod -event-msg-handler :chsk/state
  [{:as ev-msg :keys [?data]}]
  (let [[old-state-map new-state-map] (have vector? ?data)]
    (if (:first-open? new-state-map)
      (prn "Channel socket successfully established!: %s" new-state-map)
      (prn "Channel socket state change: %s"              new-state-map))))

(defmethod -event-msg-handler :chsk/recv
  [{:as ev-msg :keys [event]}]
  (let [[_ [ev-name data]] event]
    (if (= ev-name :game/broadcast-state)
      (do
        (prn "Got game state from server!")
        (rf/dispatch [:game/update-state (:db data)]))
      (prn "Push event from server: %s" ev-msg))))

(defmethod -event-msg-handler :chsk/handshake
  [{:as ev-msg :keys [?data]}]
  (let [[?uid ?csrf-token ?handshake-data] ?data]
    (prn "Handshake: %s" ?data)))

;; TODO Add your (defmethod -event-msg-handler <event-id> [ev-msg] <body>)s here...

;;;; Sente event router (our `event-msg-handler` loop)

(defonce router_ (atom nil))
(defn  stop-router! [] (when-let [stop-f @router_] (stop-f)))
(defn start-router! []
  (stop-router!)
  (reset! router_
    (sente/start-client-chsk-router!
      ch-chsk event-msg-handler)))


(defn start! [] (start-router!))

(defonce _start-once (start!))
