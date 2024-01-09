(ns td-service.components.pedestal.core
  (:require [com.stuartsierra.component :as component]
            [io.pedestal.http :as http]
            [td-service.components.pedestal.routes :as routes]))

(defn test? [config] (= :test (:env config)))

(defrecord Pedestal [config in-memory-db]
  component/Lifecycle

  (start [this]
    (println "Start http pedestal component!")
    (if (:server this)
      this
      (let [server (-> {::http/routes routes/routes
                        ::http/type   :jetty
                        ::http/join?  false
                        ::http/port   (-> config :server :port)}
                       http/default-interceptors
                       (update ::http/interceptors concat [(routes/inject-dependencies this)])
                       http/create-server)]

        (assoc this :server (cond-> server
                                    (not (test? config)) http/start)))))

  (stop [this]
    (println "Stop Pedestal!")
    (when (and (:server this) (not (test? config)))
      (http/stop (:server this)))
    (assoc this :server nil)))


(defn create-pedestal [config] (map->Pedestal {:config config}))