(ns td-service.components.pedestal
  (:require [com.stuartsierra.component :as component]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.interceptor :as interceptor]
            [io.pedestal.http.content-negotiation :as cn]
            [clojure.data.json :as json]
            [td-service.routes.list :as list]
            [td-service.routes.list-item :as item]))

(def supported-types ["application/json" "application/edn" "text/html" "text/plain"])

(def content-negotiation (cn/negotiate-content supported-types))

(defn accepted-type [ctx] (get-in ctx [:request :accept :field] "application/json"))

(defn transform-content [body content-type]
  (case content-type
    "application/json" (json/write-str body)
    body))

(defn coerce-to [response content-type]
  (-> response
      (update :body transform-content content-type)
      (assoc-in [:headers "Content-Type"] content-type)))

(def coerce-body-interceptor (interceptor/interceptor
                               {:name  ::coerce-body
                                :leave (fn [ctx]
                                         (let [content-typ (get-in ctx [:response :headers "Content-Type"])]
                                           (cond-> ctx
                                                   (nil? content-typ) (update :response coerce-to (accepted-type ctx)))))}))

(defn inject-dependencies [dependencies]
  (interceptor/interceptor
    {:name  ::inject-dependencies
     :enter #(assoc % :dependencies dependencies)}))

(defn test? [config] (= :test (:env config)))

(defn combine-routes [routes]
  (->> routes (apply concat) set route/expand-routes))

(defrecord Pedestal [config in-memory-db data-source]
  component/Lifecycle

  (start [this]
    (println "START TD_S Pedestal Component!")
    (if (:server this)
      this
      (let [server (-> {::http/routes (combine-routes [list/routes item/routes])
                        ::http/type   :jetty
                        ::http/join?  false
                        ::http/port   (-> config :server :port)}
                       http/default-interceptors
                       (update ::http/interceptors into [(inject-dependencies this) coerce-body-interceptor content-negotiation])
                       http/create-server)]

        (assoc this :server (cond-> server
                                    (not (test? config)) http/start)))))

  (stop [this]
    (println "STOP TD_S Pedestal Component!")
    (when (and (:server this) (not (test? config)))
      (http/stop (:server this)))
    (assoc this :server nil)))


(defn create-pedestal [config]
  (if config
    (map->Pedestal {:config config})
    {}))