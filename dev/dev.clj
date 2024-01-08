(ns dev
  (:require [com.stuartsierra.component.repl :as component-repl]
            [td-service.core :as core]))

(component-repl/set-init
  (fn [_old-system]
    (core/todo-service-system {:env     :dev
                               :server  {:port 3001}
                               :htmx    {:server {:port 3002}}
                               :db-spec {:jdbcUrl  "jdbc:postgresql://localhost:5432/rwa"
                                         :username "rwa"
                                         :password "rwa"}})))