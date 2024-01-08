(ns td-service.components.in-memory-db
  (:require [com.stuartsierra.component :as component]))

(defrecord InMemoryDB []
  component/Lifecycle

  (start [this]
    (println "Star InMemoryDB!")
    (assoc this :db (atom {})))

  (stop [this]
    (println "Stop InMemoryDB!")
    (assoc this :db nil)))

(defn create-in-memory-db []
  (map->InMemoryDB {}))
