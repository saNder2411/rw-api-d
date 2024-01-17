(ns dev
  (:require [com.stuartsierra.component.repl :as component-repl]
            [core :as core]))

(component-repl/set-init
  (fn [_old-system]
    (core/services-system {:td_service {:env     :dev
                                        :server  {:port 3001}
                                        :db-spec {:jdbcUrl  "jdbc:postgresql://localhost:5432/rwa"
                                                  :username "rwa"
                                                  :password "rwa"}}
                           :htmx       {:env    :dev
                                        :server {:port 3002}}})))