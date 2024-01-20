(ns td-service.components.data-source
  (:require [next.jdbc.connection :as connection])
  (:import (com.zaxxer.hikari HikariDataSource)
           (org.flywaydb.core Flyway)))


(defn create-data-source [config]
  (connection/component
    HikariDataSource
    (assoc
      (:db-spec config)
      :init-fn (fn [data-source]
                 (println "START TD_S Data-Source Component!")
                 (.migrate
                   (.. (Flyway/configure)
                       (dataSource data-source)
                       (locations (into-array String ["classpath:database/migrations"]))
                       (table "schema_version")
                       (load)))))))