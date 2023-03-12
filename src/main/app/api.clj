(ns app.api
  (:require [app.config :as config]
            [app.file-parsing :refer [parse-experiments]]
            [expound.alpha :as expound]
            [mount.core :as mount :refer [defstate]]
            [muuntaja.core :as m]
            [hiccup.core        :as hiccup]
            [org.httpkit.server :as server]
            [reitit.coercion.spec]
            [reitit.dev.pretty :as pretty]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.middleware.multipart :as multipart]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [reitit.ring.spec :as rrs]
            [reitit.spec :as rs]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.util.http-response :refer [ok]]
            [taoensso.timbre :as log]
            [taoensso.sente :as sente]
            [ring.middleware.anti-forgery]
            [ring.middleware.keyword-params]
            [ring.middleware.params]
            [ring.middleware.session]
            [taoensso.sente.server-adapters.http-kit      :refer (get-sch-adapter)])
  (:gen-class))


(let [{:keys [ch-recv
              send-fn
              connected-uids
              ajax-post-fn
              ajax-get-or-ws-handshake-fn]}
      (sente/make-channel-socket-server!
        (get-sch-adapter)
        {:csrf-token-fn nil
         :user-id-fn (fn [ring-req] (:client-id ring-req))})]
        ; {:csrf-token-fn (fn [ring-req] #p (:anti-forgery-token ring-req))})]
        ; {:csrf-token-fn
        ;  (fn [ring-req]
        ;    #p (force ring.middleware.anti-forgery/*anti-forgery-token*))))]
        ;  "testing")})]
  (def ring-ajax-post ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk ch-recv)    ; ChannelSocket's receive channel
  (def chsk-send! send-fn) ; ChannelSocket's send API fn
  (def connected-uids connected-uids)) ; Watchable, read-only atom


;; We can watch this atom for changes if we like
(add-watch connected-uids :connected-uids
  (fn [_ _ old new]
    (when (not= old new)
      (log/infof "Connected uids change: %s" new))))



(defn- reveal-information [request]
  (ok {:headers (:headers request)
       :identity (:identity request)}))


(defn landing-pg-handler [ring-req]
  (hiccup/html
    [:head
     [:title "Game"]
     [:meta {:charset "UTF-8"}]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
     [:link {:rel "stylesheet" :href "css/board.css"}]
     [:link {:rel "stylesheet" :href "css/page.css"}]
     [:link {:rel "stylesheet"
             :href "node_modules/bootstrap/dist/css/bootstrap.min.css"}]]
    [:body
      (let [csrf-token
            (:anti-forgery-token ring-req)] ; Also an option
            ; "testing"]
            ; (force ring.middleware.anti-forgery/*anti-forgery-token*)]
        (prn "TOKEN" csrf-token)
        [:div#sente-csrf-token {:data-csrf-token csrf-token}])
      [:div#app]
      [:script {:src "/js/compiled/base.js"}]]))


; Not currently necessary/used
(defn login-handler
  "Here's where you'll add your server-side login/auth procedure (Friend, etc.).
  In our simplified example we'll just always successfully authenticate the user
  with whatever user-id they provided in the auth request."
  [ring-req]
  (let [{:keys [session params]} ring-req
        {:keys [user-id]} params]
    (log/debugf "Login request: %s" params)
    {:status 200 :session (assoc session :uid user-id)}))


;;;; Sente event handlers

(defmulti -event-msg-handler
  "Multimethod to handle Sente `event-msg`s"
  :id) ; Dispatch on event-id
  

(defn event-msg-handler
  "Wraps `-event-msg-handler` with logging, error catching, etc."
  [{:as ev-msg :keys [id ?data event]}]
  (-event-msg-handler ev-msg)) ; Handle event-msgs on a single thread
  ;; (future (-event-msg-handler ev-msg)) ; Handle event-msgs on a thread pool
  

(defmethod -event-msg-handler
  :default ; Default/fallback case (no other matching handler)
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (let [session (:session ring-req)
        uid     (:uid     session)]
    (log/debugf "Unhandled event: %s" event)
    (when ?reply-fn
      (?reply-fn {:umatched-event-as-echoed-from-server event}))))

(defmethod -event-msg-handler :game/sync-state
  [ev-msg]
  (let [db (:db (:?data ev-msg))]
    (log/infof "Got game state.  Current player: %s" (:current-player-idx db))
    (doseq [uid (:any @connected-uids)]
     (chsk-send! uid
      [:game/broadcast-state
       {:db db
        :to-whom uid}]))))

;;;; Sente event router (our `event-msg-handler` loop)

(defonce router_ (atom nil))
(defn stop-sente-router! [] (when-let [stop-fn @router_] (stop-fn)))
(defn start-sente-router! []
  (stop-sente-router!)
  (reset! router_
    (sente/start-server-chsk-router!
      ch-chsk event-msg-handler)))


(def ^:private api-routes
  [["/" {:get (fn [req] (ok (landing-pg-handler req)))}]
   ["/debug"
    {:swagger {:tags ["debug"]}}
    [""
     {:name :api/debug
      :get  {:handler reveal-information}
      :post {:handler reveal-information}}]]
   ["/login" {:post login-handler}]
   ["/chsk" {:get ring-ajax-get-or-ws-handshake :post ring-ajax-post}]])



;; ----------------------------------------------------------------------------

(defn- router
  "Create a router with all routes. Configures swagger for documentation."
  []
  (ring/router
   [api-routes
    ["/swagger.json"
     {:get {:no-doc true
            :swagger {:info {:title "API"
                             :basePath "/"
                             :version "1.0.0"}}
            :handler (swagger/create-swagger-handler)}}]]
   {:exception pretty/exception
    :conflicts (constantly nil)
    :validate rrs/validate
    ::rs/explain expound/expound-str
    :data {:coercion reitit.coercion.spec/coercion
           :muuntaja m/instance
           :middleware [swagger/swagger-feature
                        parameters/parameters-middleware ;; query-params & form-params
                        muuntaja/format-middleware
                        ring.middleware.keyword-params/wrap-keyword-params
                        ring.middleware.params/wrap-params
                        ; ring.middleware.anti-forgery/wrap-anti-forgery
                        ring.middleware.session/wrap-session
                        coercion/coerce-response-middleware ;; coercing response bodies
                        coercion/coerce-request-middleware ;; coercing request parameters
                        multipart/multipart-middleware]}}))

(defn app
  []
  (ring/ring-handler
   (router)
   (ring/routes
    ; https://github.com/metosin/reitit/blob/master/doc/ring/static.md#internal-routes
    ; Used to serve all the js/css files.
    (ring/create-resource-handler {:path "/"})
    (swagger-ui/create-swagger-ui-handler
     {:path "/swagger"
      :config {:validatorUrl nil
               :operationsSorter "alpha"}})
    (ring/redirect-trailing-slash-handler {:method :strip})
    (ring/create-default-handler))))

(def allowed-http-verbs
  #{:get :put :post :delete :options})

(defstate api
  :start
  (let [origins #".*"]
    (log/info (format "Allowed Origins: %s" origins))
    (log/info (format "Find the backend with swagger documentation at %s%s" config/api-location "/swagger"))
    (start-sente-router!)
    (server/run-server
     (wrap-cors (app)
                :access-control-allow-origin origins
                :access-control-allow-methods allowed-http-verbs)
     {:port config/api-port}))
  :stop (when api (api :timeout 1000)))

(defn -main
  "This is our main entry point for the REST API Server."
  [& _args]
  (log/info (mount/start)))

(comment
  "Start the server from here"
  (-main)
  (mount/start)
  (mount/stop)
  :end)
