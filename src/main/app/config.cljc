(ns app.config)

(def api-port 3000)


(def api-host
; when developing locally, uncomment this
; TODO use environment variables or something to auto set this
  ; "localhost")
  "kovas.duckdns.org")

(def api-location
  (str "http://" api-host ":" api-port))
