(ns td-service.components.pedestal.routes
  (:require [io.pedestal.http.route :as route]))

(defn respond-hello [_request]
  {:status 200 :body "Hello, Clojure!"})

(def routes (route/expand-routes
              #{["/greet" :get respond-hello :route-name :greet]}))
