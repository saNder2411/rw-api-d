(ns td-service.routes.list
  (:require [io.pedestal.http.route :as route]
            [io.pedestal.interceptor :as interceptor]
            [td-service.db.atom-db :as m-db]))

(defn response [status body & {:as headers}]
  {:status status :body body :headers headers})

(def ok (partial response 200))
(def created (partial response 201))


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


(def routes
  #{["/todos" :post [entity-render db-interceptor list-create]]
    ["/todos" :get [entity-render db-interceptor lists-view]]
    ["/todos/:list-id" :get [entity-render db-interceptor list-view]]
    ["/todos/:list-id" :put [entity-render list-view db-interceptor list-update]]
    ["/todos/:list-id" :delete [entity-render lists-view db-interceptor list-delete]]})
