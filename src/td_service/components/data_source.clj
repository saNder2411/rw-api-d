(ns td-service.components.data-source
  (:require [next.jdbc.connection :as connection])
  (:import (com.zaxxer.hikari HikariDataSource)))


(defn create-data-source [config]
  (connection/component HikariDataSource (:db-spec config)))