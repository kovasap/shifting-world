(ns app.config)

(def api-port 3000)

; when developing locally, uncomment this
; TODO use environment variables or something to auto set this
; (def api-host
;   "localhost")

(def api-host
  "kovas.duckdns.org")

(def api-location
  (str "http://" api-host ":" api-port))
