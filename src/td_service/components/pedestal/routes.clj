(ns td-service.components.pedestal.routes
  (:require [io.pedestal.http.route :as route]
            [io.pedestal.http :as http]
            [td-service.db.atom-db :as m-db]))

(defn response [status body & {:as headers}]
  {:status status :body body :headers headers})

(def ok (partial response 200))
(def created (partial response 201))



(def db-interceptor {:name  :db-interceptor
                     :enter #(update % :request assoc :database @m-db/atom-db)
                     :leave (fn [ctx]
                              (if-let [[operation & args] (:tx-data ctx)]
                                (do
                                  (apply swap! m-db/atom-db operation args)
                                  (assoc-in ctx [:request :database] @m-db/atom-db))
                                ctx))})

(def entity-render {:name  :entity-render
                    :leave (fn [ctx]
                             (if-let [{:keys [created? body headers]} (:result ctx)]
                               (let [response-fn (if created? created ok)]
                                 (assoc ctx :response (apply response-fn body headers)))
                               ctx))})

(def lists-view {:name  :lists-view
                 :enter (fn [ctx]
                          (assoc ctx :result {:body (get-in ctx [:request :database])}))})

(def list-view {:name  :list-view
                :leave (fn [ctx]
                         (let [id (get-in ctx [:request :path-params :list-id])
                               the-list (and id (m-db/get-list-by-id (get-in ctx [:request :database]) id))]
                           (cond-> ctx
                                   the-list (assoc :result {:body the-list}))))})

(def list-create {:name  :list-create
                  :enter (fn [ctx]
                           (let [title (get-in ctx [:request :query-params :title] "Unnamed List")
                                 new-list (m-db/make-list title)
                                 db-id (str (gensym "l-"))
                                 url (route/url-for :list-view :params {:list-id db-id})]
                             (assoc ctx :result {:created? true :body new-list :headers ["Location" url]}
                                        :tx-data [assoc db-id new-list])))})

(def list-update {:name  :list-update
                  :enter (fn [ctx]
                           (let [list-id (get-in ctx [:request :path-params :list-id])
                                 new-list (get-in ctx [:request :query-params] {})]
                             (cond-> ctx
                                     list-id (assoc :tx-data [m-db/update-list list-id new-list]))))})

(def list-delete {:name  :list-delete
                  :enter (fn [ctx]
                           (let [list-id (get-in ctx [:request :path-params :list-id])]
                             (cond-> ctx
                                     list-id (assoc :tx-data [m-db/delete-list list-id]))))})

(def list-item-view {:name  :list-item-view
                     :leave (fn [ctx]
                              (let [l-id (get-in ctx [:request :path-params :list-id])
                                    i-id (and l-id (get-in ctx [:request :path-params :item-id]))
                                    item (and i-id (m-db/get-list-item-by-id (get-in ctx [:request :database]) l-id i-id))]
                                (cond-> ctx
                                        item (assoc :result {:body item}))))})

(def list-item-create {:name  :list-item-create
                       :enter (fn [ctx]
                                (if-let [list-id (get-in ctx [:request :path-params :list-id])]
                                  (let [title (get-in ctx [:request :query-params :title] "Unnamed Item")
                                        new-item (m-db/make-list-item title list-id)
                                        db-id (str list-id (gensym "-i-"))
                                        url (route/url-for :list-item-view :params {:list-id list-id :item-id db-id})]
                                    (assoc ctx :result {:created? true :body new-item :headers ["Location" url]}
                                               :tx-data [m-db/create-list-item list-id db-id new-item]))
                                  ctx))})

(def list-item-update {:name  :list-item-update
                       :enter (fn [ctx]
                                (let [{:keys [list-id item-id]} (get-in ctx [:request :path-params])
                                      new-item (get-in ctx [:request :query-params] {})]
                                  (cond-> ctx
                                          (and list-id item-id) (assoc :tx-data [m-db/update-list-item list-id item-id new-item]))))})

(def list-item-delete {:name  :list-item-delete
                       :enter (fn [ctx]
                                (let [{:keys [list-id item-id]} (get-in ctx [:request :path-params])]
                                  (cond-> ctx
                                          (and list-id item-id) (assoc :tx-data [m-db/delete-list-item list-id item-id]))))})

(defn respond-hello [_request]
  {:status 200 :body "Hello, Clojure!"})

(def routes
  (route/expand-routes
    #{["/greet" :get respond-hello :route-name :greet]

      ["/todo" :get [entity-render db-interceptor lists-view]]
      ["/todo" :post [entity-render db-interceptor list-create]]
      ["/todo/:list-id" :get [entity-render db-interceptor list-view]]
      ["/todo/:list-id" :put [entity-render list-view db-interceptor list-update]]
      ["/todo/:list-id" :delete [entity-render lists-view db-interceptor list-delete]]

      ["/todo/:list-id" :post [entity-render db-interceptor list-item-create]]
      ["/todo/:list-id/:item-id" :get [entity-render db-interceptor list-item-view]]
      ["/todo/:list-id/:item-id" :put [entity-render list-item-view db-interceptor list-item-update]]
      ["/todo/:list-id/:item-id" :delete [entity-render list-view db-interceptor list-item-delete]]}))
