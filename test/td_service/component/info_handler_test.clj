(ns td-service.component.info-handler-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [io.pedestal.test :refer [response-for]]
            [core :as core]
            [td-service.utils.test-helpers :as h])
  (:import (org.testcontainers.containers PostgreSQLContainer)))

(def sut (gensym "sut"))

(deftest info-handler-test
  (let [db-container (doto (PostgreSQLContainer. "postgres:15.4")
                       (.withDatabaseName "rwa-test")
                       (.withUsername "test")
                       (.withPassword "test"))]
    (try
      (.start db-container)
      (h/with-system [sut (core/services-system {:td-service {:env     :test
                                                              :server  {:port 3001}
                                                              :db-spec {:jdbcUrl  (.getJdbcUrl db-container)
                                                                        :username (.getUsername db-container)
                                                                        :password (.getPassword db-container)}}})]
                     (let [service (h/service-fn sut :td-pedestal)
                           url-for (h/url-for-routes sut :td-pedestal)]
                       (testing "/info/todos :get"
                         (let [url (url-for :info-handler)
                               {:keys [status body]} (response-for service :get url :headers {"Accept" "text/plain"})]

                           (is (= 200 status))
                           (is (= "Database server version: 15.4 (Debian 15.4-2.pgdg120+1)" body))))))
      (finally
        (.stop db-container)))))


(comment
  (run-tests)
  )