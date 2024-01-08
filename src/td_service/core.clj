(ns td-service.core
  (:require [td-service.config :as config]
            [com.stuartsierra.component :as component]
            [td-service.components.in-memory-db :as in-memory-db]
            [td-service.components.pedestal.core :as pedestal]))


(defn todo-service-system [config]
  (component/system-map
    :in-memory-db (in-memory-db/create-in-memory-db)
    :pedestal (component/using
                (pedestal/create-pedestal config)
                [:in-memory-db])))

(defn -main []
  (let [config (config/read-config)
        system (-> config todo-service-system component/start-system)]
    (println "Starting Real Worlds API Service with config" config)
    (.addShutdownHook
      (Runtime/getRuntime)
      (new Thread #(component/stop-system system)))))