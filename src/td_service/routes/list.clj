(ns td-service.routes.list
  (:require [io.pedestal.http.route :as route]
            [io.pedestal.interceptor :as interceptor]
            [next.jdbc :as jdbc]
            [td-service.db.postgresql :as db]))

(defn response [status body & {:as headers}]
  {:status status :body body :headers headers})

(def ok (partial response 200))
(def created (partial response 201))

(def db-in-memory-interceptor (interceptor/interceptor
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

(def db-interceptor (interceptor/interceptor
                       {:name  :db-interceptor
                        :leave (fn [ctx]
                                 (if-let [[operation & args] (:tx-data ctx)]
                                   (let [data-source (get-in ctx [:dependencies :data-source])
                                         result (apply operation data-source args)]
                                     (cond-> ctx
                                             result (assoc :result {:body result})))
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
                             (let [title (get-in ctx [:request :query-params :title] "Unnamed List")
                                   data-source (get-in ctx [:dependencies :data-source])
                                   result (db/create-el data-source :lists [(db/make-list title)])
                                   url (route/url-for :list-view :params {:list-id (:id result)})]
                               (assoc ctx :result {:created? true :body result :headers ["Location" url]})))}))

(def lists-view (interceptor/interceptor
                  {:name  :lists-view
                   :leave (fn [ctx]
                            (assoc ctx :tx-data [db/find-all :lists]))}))

(def list-view (interceptor/interceptor
                 {:name  :list-view
                  :leave (fn [ctx]
                           (let [id (get-in ctx [:request :path-params :list-id])]
                             (assoc ctx :tx-data [db/find-by-id :lists id])))}))

(def list-update (interceptor/interceptor
                   {:name  :list-update
                    :enter (fn [ctx]
                             (let [id (get-in ctx [:request :path-params :list-id])
                                   new-state (get-in ctx [:request :query-params] {})]
                               (assoc ctx :tx-data [db/update-el :lists new-state id])))}))

(def list-delete (interceptor/interceptor
                   {:name  :list-delete
                    :enter (fn [ctx]
                             (let [list-id (get-in ctx [:request :path-params :list-id])]
                               (assoc ctx :tx-data [db/delete-el :lists list-id])))}))



(def routes
  #{["/todos" :post [entity-render list-create]]
    ["/todos" :get [entity-render db-interceptor lists-view]]
    ["/todos/:list-id" :get [entity-render db-interceptor list-view]]
    ["/todos/:list-id" :put [entity-render db-interceptor list-update]]
    ["/todos/:list-id" :delete [entity-render db-interceptor list-delete]]})
