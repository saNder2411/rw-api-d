(ns htmx.components.htmx-pedestal
  (:require [com.stuartsierra.component :as component]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.interceptor :as interceptor]
            [htmx.routes.click-to-edit :as click-to-edit]
            [htmx.routes.infinite-scroll :as infinite-scroll]
            [htmx.routes.active-search :as active-search]
            [htmx.routes.delete-with-confirmation :as delete-with-confirmation]
            [htmx.routes.comments-section :as comments-section]))

(def routes
  (route/expand-routes
    (into #{}
          (concat click-to-edit/routes
                  infinite-scroll/routes
                  active-search/routes
                  delete-with-confirmation/routes
                  comments-section/routes))))

(defn inject-dependencies
  [dependencies]
  (interceptor/interceptor
    {:name  ::inject-dependencies
     :enter (fn [context]
              (assoc context :dependencies dependencies))}))

(defn test? [config] (= :test (:env config)))

(defrecord HTMXPedestal [config htmx-in-memory-db]
  component/Lifecycle
  (start [this]
    (println "Starting HTMXPedestalComponent")
    (if (:server this)
      this
      (let [server (-> {::http/routes         routes
                        ::http/type           :jetty
                        ::http/join?          false
                        ::http/port           (-> config :server :port)
                        ::http/secure-headers {:content-security-policy-settings {:object-src "none"}}}
                       http/default-interceptors
                       (update ::http/interceptors into [(inject-dependencies this)])
                       http/create-server)]
        (assoc this :server (cond-> server
                                    (not (test? config)) http/start)))))
  (stop [this]
    (println "Stopping HTMXPedestalComponent")
    (when (and (:server this) (not (test? config)))
      (http/stop (:server this)))
    (assoc this :server nil)))

(defn create-htmx-pedestal [config]
  (if config
    (map->HTMXPedestal {:config config})
    {}))