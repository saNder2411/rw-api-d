(ns td-service.routes.list-item
  (:require [io.pedestal.http.route :as route]
            [io.pedestal.interceptor :as interceptor]
            [io.pedestal.http.body-params :as body-params]
            [td-service.db.postgresql :as db]
            [td-service.routes.list :as l]
            [schema.core :as s]
            [malli.core :as m]))

(s/defschema ItemJson
  {:title s/Str
   :done  s/Bool})

(def ItemJsonSchema
  (m/schema
    [:map
     [:title :string]
     [:done :boolean]]))

(def list-item-create (interceptor/interceptor
                        {:name  :list-item-create
                         :enter (fn [ctx]
                                  (if-let [list-id (get-in ctx [:request :path-params :list-id])]
                                    (let [data-source (get-in ctx [:dependencies :data-source])
                                          json-body (get-in ctx [:request :json-params])
                                          result (db/create-el data-source :items [(db/make-item list-id (s/validate ItemJson json-body))])
                                          url (route/url-for :list-item-view :params {:list-id list-id :item-id (:id result)})]
                                      (assoc ctx :result {:created? true :body result :headers ["Location" url]}))
                                    ctx))}))

(def list-item-view (interceptor/interceptor
                      {:name  :list-item-view
                       :leave (fn [ctx]
                                (let [id (get-in ctx [:request :path-params :item-id])]
                                  (assoc ctx :tx-data [db/find-by-id :items id])))}))

(def list-item-update (interceptor/interceptor
                        {:name  :list-item-update
                         :enter (fn [ctx]
                                  (let [{:keys [item-id]} (get-in ctx [:request :path-params])
                                        new-state (get-in ctx [:request :query-params] {})]
                                    (assoc ctx :tx-data [db/update-el :items new-state item-id])))}))

(def list-item-delete (interceptor/interceptor
                        {:name  :list-item-delete
                         :enter (fn [ctx]
                                  (let [{:keys [item-id]} (get-in ctx [:request :path-params])]
                                    (assoc ctx :tx-data [db/delete-el :items item-id])))}))

(def routes
  #{["/todos/:list-id" :post [(body-params/body-params) l/entity-render l/db-interceptor list-item-create]]
    ["/todos/:list-id/:item-id" :get [l/entity-render l/db-interceptor list-item-view]]
    ["/todos/:list-id/:item-id" :put [(body-params/body-params) l/entity-render list-item-view l/db-interceptor list-item-update]]
    ["/todos/:list-id/:item-id" :delete [l/entity-render l/list-view l/db-interceptor list-item-delete]]})
