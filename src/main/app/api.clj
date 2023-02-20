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
      (sente/make-channel-socket! (get-sch-adapter)
                                  {:csrf-token-fn (fn [ring-req] "testing")})]
  (def ring-ajax-post ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk ch-recv)    ; ChannelSocket's receive channel
  (def chsk-send! send-fn) ; ChannelSocket's send API fn
  (def connected-uids connected-uids)) ; Watchable, read-only atom



(defn- reveal-information [request]
  (ok {:headers (:headers request)
       :identity (:identity request)}))


(defn landing-pg-handler [] ;  [ring-req]
  (hiccup/html
    [:head
     [:title "Game"]
     [:meta {:charset "UTF-8"}]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
     [:link {:rel "stylesheet" :href "css/board.css"}]
     [:link {:rel "stylesheet"
             :href "node_modules/bootstrap/dist/css/bootstrap.min.css"}]]
    [:body
      (let [csrf-token
            ;; (:anti-forgery-token ring-req) ; Also an option
            "testing"]
            ; (force ring.middleware.anti-forgery/*anti-forgery-token*)]

        [:div#sente-csrf-token {:data-csrf-token csrf-token}])
      [:div#app]
      [:script {:src "/js/compiled/base.js"}]]))


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

(defmethod -event-msg-handler :game/send-state
  [ev-msg] (log/infof "Got game state: %s" (:db (:?data ev-msg))))

;;;; Sente event router (our `event-msg-handler` loop)

(defonce router_ (atom nil))
(defn stop-sente-router! [] (when-let [stop-fn @router_] (stop-fn)))
(defn start-sente-router! []
  (stop-sente-router!)
  (reset! router_
    (sente/start-server-chsk-router!
      ch-chsk event-msg-handler)))


(def ^:private api-routes
  [["/" {:get (fn [_] (ok (landing-pg-handler)))}]
   ["/debug" {:swagger {:tags ["debug"]}}
    ["" {:name :api/debug
         :get {:handler reveal-information}
         :post {:handler reveal-information}}]]
   ["/wizard"
    {:get (fn [_] (ok {:wizard "🧙"}))}]
   ["/experiments"
    {:get (fn [_] (ok {:experiments (parse-experiments "./data")}))}]
   ["/chsk"
    {:get (fn [req] (ring-ajax-get-or-ws-handshake req))
     :post (fn [req] (ring-ajax-post req))}]])



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
                        ring.middleware.anti-forgery/wrap-anti-forgery
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
