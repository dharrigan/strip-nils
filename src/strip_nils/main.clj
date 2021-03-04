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

(def app
  (ring/ring-handler
   (ring/router
    ["/" {:get (fn [request]
                 (let [{{{:keys [msg]} :query} :parameters} request]
                   {:status 200 :body {:foo msg :foo-2 {:msg msg} :bar "Hello World"}}))
          :parameters {:query [:map
                               {:closed true}
                               [:msg {:optional true} string?]]}}]
    {:data {:coercion rcm/coercion
            :muuntaja (m/create
                       (assoc-in m/default-options
                                 [:formats "application/json" :opts]
                                 {:mapper (-> (j/object-mapper)
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

 ;; An example using `httpie`
 ;;
 ;; ❯ http --verbose localhost:8080
 ;;
 ;; GET / HTTP/1.1
 ;; Accept: */*
 ;; Accept-Encoding: gzip, deflate
 ;; Connection: keep-alive
 ;; Host: localhost:8080
 ;; User-Agent: HTTPie/2.4.0



 ;; HTTP/1.1 200 OK
 ;; Content-Length: 21
 ;; Content-Type: application/json;charset=utf-8
 ;; Date: Thu, 04 Mar 2021 10:45:40 GMT
 ;; Server: Jetty(9.4.36.v20210114)

 ;; {
 ;;     "bar": "Hello World"
 ;; }

 ;; Second Example (with a parameter, to show non-nil values)

 ;; ❯ http --verbose localhost:8080 msg==hello
 ;;
 ;; GET /?msg=hello HTTP/1.1
 ;; Accept: */*
 ;; Accept-Encoding: gzip, deflate
 ;; Connection: keep-alive
 ;; Host: localhost:8080
 ;; User-Agent: HTTPie/2.4.0

 ;; HTTP/1.1 200 OK
 ;; Content-Length: 59
 ;; Content-Type: application/json;charset=utf-8
 ;; Date: Thu, 04 Mar 2021 10:46:23 GMT
 ;; Server: Jetty(9.4.36.v20210114)

 ;; {
 ;;     "bar": "Hello World",
 ;;     "foo": "hello",
 ;;     "foo-2": {
 ;;         "msg": "hello"
 ;;     }
 ;; }



 ,)
