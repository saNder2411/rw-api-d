(ns core
  (:require [config :as config]
            [com.stuartsierra.component :as component]
            [td-service.components.in-memory-db :as in-memory-db]
            [td-service.components.data-source :as data-source]
            [td-service.components.pedestal :as pedestal]
            [htmx.components.htmx-in-memory-db :as htmx-in-memory-db]
            [htmx.components.htmx-pedestal :as htmx-pedestal]))


(defn services-system [config]
  (component/system-map
    :in-memory-db (in-memory-db/create-in-memory-db)
    :data-source (data-source/create-data-source (:td-service config))
    :td-pedestal (component/using
                   (pedestal/create-pedestal (:td-service config))
                   [:in-memory-db :data-source])

    :htmx-in-memory-db (htmx-in-memory-db/create-htmx-in-memory-db)
    :htmx-pedestal (component/using
                     (htmx-pedestal/create-htmx-pedestal (:htmx config))
                     [:htmx-in-memory-db])))

(defn -main []
  (let [config (config/read-config)
        system (-> config services-system component/start-system)]
    (println "Starting Real Worlds API Service with config" config)
    (.addShutdownHook
      (Runtime/getRuntime)
      (new Thread #(component/stop-system system)))))