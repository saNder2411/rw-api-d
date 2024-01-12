(ns td-service.components.pedestal.routes
  (:require [io.pedestal.http.route :as route]
            [clojure.data.json :as json]
            [io.pedestal.interceptor :as interceptor]
            [io.pedestal.http.content-negotiation :as cn]
            [io.pedestal.http.body-params :as body-params]
            [td-service.db.atom-db :as m-db]
            [schema.core :as s]))

(defn response [status body & {:as headers}]
  {:status status :body body :headers headers})

(def ok (partial response 200))
(def created (partial response 201))

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

(def db-interceptor (interceptor/interceptor
                      {:name  :db-interceptor
                       :enter (fn [ctx]
                                (let [atom-db (get-in ctx [:dependencies :in-memory-db :db])]
                                  (update ctx :request assoc :database @atom-db)))

                       :leave (fn [ctx]
                                (if-let [[operation & args] (:tx-data ctx)]
                                  (let [atom-db (get-in ctx [:dependencies :in-memory-db :db])]
                                    (apply swap! atom-db operation args)
                                    (assoc-in ctx [:request :database] @atom-db))
                                  ctx))}))

(def entity-render (interceptor/interceptor
                     {:name  :entity-render
                      :leave (fn [ctx]
                               (if-let [{:keys [created? body headers]} (:result ctx)]
                                 (let [response-fn (if created? created ok)]
                                   (assoc ctx :response (apply response-fn body headers)))
                                 ctx))}))

(def list-create (interceptor/interceptor
                   {:name  :list-create
                    :enter (fn [ctx]
                             (let [nm (get-in ctx [:request :query-params :name] "Unnamed List")
                                   db-id (str (random-uuid))
                                   new-list (m-db/make-list db-id nm)
                                   url (route/url-for :list-view :params {:list-id db-id})]
                               (assoc ctx :result {:created? true :body new-list :headers ["Location" url]}
                                          :tx-data [assoc-in [:list-collection db-id] new-list])))}))

(def lists-view (interceptor/interceptor
                  {:name  :lists-view
                   :leave (fn [ctx]
                            (assoc ctx :result {:body (get-in ctx [:request :database :list-collection])}))}))

(def list-view (interceptor/interceptor
                 {:name  :list-view
                  :leave (fn [ctx]
                           (let [id (get-in ctx [:request :path-params :list-id])
                                 the-list (and id (get-in ctx [:request :database :list-collection id]))]
                             (cond-> ctx
                                     the-list (assoc :result {:body the-list}))))}))


(def list-update (interceptor/interceptor
                   {:name  :list-update
                    :enter (fn [ctx]
                             (let [list-id (get-in ctx [:request :path-params :list-id])
                                   new-state (get-in ctx [:request :query-params] {})]
                               (cond-> ctx
                                       list-id (assoc :tx-data [m-db/update-element :list-collection list-id new-state]))))}))

(def list-delete (interceptor/interceptor
                   {:name  :list-delete
                    :enter (fn [ctx]
                             (let [list-id (get-in ctx [:request :path-params :list-id])]
                               (cond-> ctx
                                       list-id (assoc :tx-data [m-db/delete-element :list-collection list-id]))))}))

(s/defschema ItemJson
  {:name  s/Str
   :done? s/Bool})

(def list-item-create (interceptor/interceptor
                        {:name  :list-item-create
                         :enter (fn [ctx]
                                  (if-let [list-id (get-in ctx [:request :path-params :list-id])]
                                    (let [json-body (get-in ctx [:request :json-params])
                                          db-id (str (random-uuid))
                                          new-item (m-db/make-item db-id list-id (s/validate ItemJson json-body))
                                          url (route/url-for :list-item-view :params {:list-id list-id :item-id db-id})]
                                      (assoc ctx :result {:created? true :body new-item :headers ["Location" url]}
                                                 :tx-data [m-db/create-item-el list-id db-id new-item]))
                                    ctx))}))

(def list-item-view (interceptor/interceptor
                      {:name  :list-item-view
                       :leave (fn [ctx]
                                (let [id (get-in ctx [:request :path-params :item-id])
                                      item (get-in ctx [:request :database :item-collection id])]
                                  (cond-> ctx
                                          item (assoc :result {:body item}))))}))


(def list-item-update (interceptor/interceptor
                        {:name  :list-item-update
                         :enter (fn [ctx]
                                  (let [{:keys [item-id]} (get-in ctx [:request :path-params])
                                        new-state (get-in ctx [:request :query-params] {})]
                                    (cond-> ctx
                                            item-id (assoc :tx-data [m-db/update-element :item-collection item-id new-state]))))}))

(def list-item-delete (interceptor/interceptor
                        {:name  :list-item-delete
                         :enter (fn [ctx]
                                  (let [{:keys [list-id item-id]} (get-in ctx [:request :path-params])]
                                    (cond-> ctx
                                            item-id (assoc :tx-data [m-db/delete-item-el list-id item-id]))))}))

(def routes
  (route/expand-routes
    #{["/todo" :post [entity-render db-interceptor list-create]]
      ["/todo" :get [entity-render db-interceptor lists-view]]
      ["/todo/:list-id" :get [entity-render db-interceptor list-view]]
      ["/todo/:list-id" :put [entity-render list-view db-interceptor list-update]]
      ["/todo/:list-id" :delete [entity-render lists-view db-interceptor list-delete]]

      ["/todo/:list-id" :post [(body-params/body-params) entity-render db-interceptor list-item-create]]
      ["/todo/:list-id/:item-id" :get [entity-render db-interceptor list-item-view]]
      ["/todo/:list-id/:item-id" :put [(body-params/body-params) entity-render list-item-view db-interceptor list-item-update]]
      ["/todo/:list-id/:item-id" :delete [entity-render list-view db-interceptor list-item-delete]]}))
