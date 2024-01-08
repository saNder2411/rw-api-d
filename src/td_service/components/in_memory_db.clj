(ns td-service.components.in-memory-db
  (:require [com.stuartsierra.component :as component]
            [td-service.db.atom-db :as db]))



(defrecord InMemoryDB []
  component/Lifecycle

  (start [this]
    (println "Star InMemoryDB!")
    (assoc this :db db/atom-db))

  (stop [this]
    (println "Stop InMemoryDB!")
    (assoc this :db nil)))

(defn create-in-memory-db []
  (map->InMemoryDB {}))
