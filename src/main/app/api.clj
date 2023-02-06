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


(let [{:keys [ch-recv send-fn connected-uids
              ajax-post-fn ajax-get-or-ws-handshake-fn]}
      (sente/make-channel-socket! (get-sch-adapter) {})]
  (def ring-ajax-post                ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk                       ch-recv) ; ChannelSocket's receive channel
  (def chsk-send!                    send-fn) ; ChannelSocket's send API fn
  (def connected-uids                connected-uids)) ; Watchable, read-only atom



(defn- reveal-information [request]
  (ok {:headers (:headers request)
       :identity (:identity request)}))


(defn landing-pg-handler [] ;  [ring-req]
  #p (hiccup/html
       [:h1 "Sente reference example"]
       (let [csrf-token
             ;; (:anti-forgery-token ring-req) ; Also an option
             (force ring.middleware.anti-forgery/*anti-forgery-token*)]

         [:div#sente-csrf-token {:data-csrf-token csrf-token}])
       [:p "An Ajax/WebSocket" [:strong " (random choice!)"] " has been configured for this example"]
       [:hr]
       [:p [:strong "Step 1: "] " try hitting the buttons:"]
       [:p
        [:button#btn1 {:type "button"} "chsk-send! (w/o reply)"]
        [:button#btn2 {:type "button"} "chsk-send! (with reply)"]]
       [:p
        [:button#btn3 {:type "button"} "Test rapid server>user async pushes"]
        [:button#btn4 {:type "button"} "Toggle server>user async broadcast push loop"]]
       [:p
        [:button#btn5 {:type "button"} "Disconnect"]
        [:button#btn6 {:type "button"} "Reconnect"]]
       ;;
       [:p [:strong "Step 2: "] " observe std-out (for server output) and below (for client output):"]
       [:textarea#output {:style "width: 100%; height: 200px;"}]
       ;;
       [:hr]
       [:h2 "Step 3: try login with a user-id"]
       [:p  "The server can use this id to send events to *you* specifically."]
       [:p
        [:input#input-login {:type :text :placeholder "User-id"}]
        [:button#btn-login {:type "button"} "Secure login!"]]
       ;;
       [:hr]
       [:h2 "Step 4: want to re-randomize Ajax/WebSocket connection type?"]
       [:p "Hit your browser's reload/refresh button"]
       [:script {:src "main.js"}])) ; Include our cljs target


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
    (swagger-ui/create-swagger-ui-handler
     {:path "/"
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
    (log/info (format "Find the backend with swagger documentation at %s" config/api-location))
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
