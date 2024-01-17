(ns td-service.routes.list-item
  (:require [io.pedestal.http.route :as route]
            [io.pedestal.interceptor :as interceptor]
            [io.pedestal.http.body-params :as body-params]
            [td-service.db.atom-db :as m-db]
            [td-service.routes.list :as l]
            [schema.core :as s]))

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
  #{["/todos/:list-id" :post [(body-params/body-params) l/entity-render l/db-interceptor list-item-create]]
    ["/todos/:list-id/:item-id" :get [l/entity-render l/db-interceptor list-item-view]]
    ["/todos/:list-id/:item-id" :put [(body-params/body-params) l/entity-render list-item-view l/db-interceptor list-item-update]]
    ["/todos/:list-id/:item-id" :delete [l/entity-render l/list-view l/db-interceptor list-item-delete]]})
