(ns htmx.components.htmx-in-memory-db
  (:require [com.stuartsierra.component :as component]
            [htmx.db.atom-db :as db]))

(defrecord HTMXInMemoryDB []
  component/Lifecycle

  (start [this]
    (println "Star HTMX InMemoryDB!")
    (assoc this :htmx-click-to-edit-state db/atom-db))

  (stop [this]
    (println "Stop HTMX InMemoryDB!")
    (assoc this :htmx-click-to-edit-state nil)))

(defn create-htmx-in-memory-db []
  (map->HTMXInMemoryDB {}))
