(ns strip-nils.main
  (:require
   [jsonista.core :as j]
   [muuntaja.core :as m]
   [reitit.coercion.malli :as rcm]
   [reitit.ring :as ring]
   [reitit.ring.coercion :as coercion]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]
   [ring.adapter.jetty :as jetty])
  (:import
   [org.eclipse.jetty.server Server]
   [com.fasterxml.jackson.annotation JsonInclude$Include]))

(defn response
  [msg]
  {:hello msg :bar nil :baz [] :wibble #{} :quux ["1"]})

(def app
  (ring/ring-handler
   (ring/router
    ["/" {:get {:handler (fn [request]
                           (let [{{{:keys [msg]} :query} :parameters} request]
                             {:status 200 :body (response msg)}))
                :parameters {:query [:map
                                     {:closed true}
                                     [:msg {:optional true} string?]]}}
          :post {:handler (fn [request]
                            (let [{{{:keys [msg]} :body} :parameters} request]
                              {:status 201 :body (response msg)}))
                 :parameters {:body [:map
                                     {:closed true}
                                     [:msg string?]]}}}]
    {:data {:coercion rcm/coercion
            ;:muuntaja m/instance
            :muuntaja (m/create
                       (assoc-in m/default-options
                                 [:formats "application/json" :opts]
                                 {:mapper (-> (j/object-mapper {:decode-key-fn true})
                                              (.setSerializationInclusion JsonInclude$Include/NON_EMPTY))}))
            :middleware [muuntaja/format-middleware
                         parameters/parameters-middleware
                         coercion/coerce-request-middleware
                         coercion/coerce-response-middleware]}})))

(defn jetty-start
  []
  (jetty/run-jetty #'app {:port 8080 :join? false}))

(defn jetty-stop
  [^Server server]
  (.stop server)
  (.join server))

(comment

 (def server (jetty-start))

 (jetty-stop server)

 ;; Examples using `httpie`

 ;; ❯ http --verbose :8080 msg==world
 ;; GET /?msg=world HTTP/1.1
 ;; Accept: */*
 ;; Accept-Encoding: gzip, deflate
 ;; Connection: keep-alive
 ;; Host: localhost:8080
 ;; User-Agent: HTTPie/2.4.0
 ;;
 ;;
 ;;
 ;; HTTP/1.1 200 OK
 ;; Content-Length: 30
 ;; Content-Type: application/json;charset=utf-8
 ;; Date: Tue, 25 May 2021 12:43:14 GMT
 ;; Server: Jetty(9.4.40.v20210413)
 ;;
 ;; {
 ;;     "hello": "world",
 ;;     "quux": [
 ;;         "1"
 ;;     ]
 ;; }
 ;;
 ;; ❯ http --verbose POST :8080 msg=world
 ;; POST / HTTP/1.1
 ;; Accept: application/json, */*;q=0.5
 ;; Accept-Encoding: gzip, deflate
 ;; Connection: keep-alive
 ;; Content-Length: 16
 ;; Content-Type: application/json
 ;; Host: localhost:8080
 ;; User-Agent: HTTPie/2.4.0
 ;;
 ;; {
 ;;     "msg": "world"
 ;; }
 ;;
 ;;
 ;; HTTP/1.1 201 Created
 ;; Content-Length: 30
 ;; Content-Type: application/json;charset=utf-8
 ;; Date: Tue, 25 May 2021 12:36:49 GMT
 ;; Server: Jetty(9.4.40.v20210413)
 ;;
 ;; {
 ;;     "hello": "world",
 ;;     "quux": [
 ;;         "1"
 ;;

 ,)
