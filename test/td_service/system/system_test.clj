(ns td-service.system.system-test
  (:require [clojure.test :refer [deftest testing is run-tests]]
            [io.pedestal.test :refer [response-for]]
            [utils.helpers :as h]
            [td-service.db.atom-db :refer [test-list-id test-item-id test-list]]
            [td-service.core :as core]))

(def sut (gensym "sut"))

(def config {:env     :test
             :server  {:port 3001}
             :htmx    {:server {:port 3002}}
             :db-spec {:jdbcUrl  "jdbc:postgresql://localhost:5432/rwa"
                       :username "rwa"
                       :password "rwa"}})

(deftest system-test
  (testing "/todo :get"
    (h/with-system [sut (core/todo-service-system config)]
                   (let [service (h/service-fn sut)
                         {:keys [status body]} (response-for service :get (h/url-for :lists-view))]
                     (is (= 200 status))
                     (is (= (str {test-list-id test-list}) body)))))

  (testing "/todo :get"
    (h/with-system [sut (core/todo-service-system config)]
                   (let [service (h/service-fn sut)
                         {:keys [status body]} (response-for service :get (h/url-for :lists-view))]
                     (is (= 200 status))
                     (is (= (str {test-list-id test-list}) body)))))

  (testing "/todo/:list-id :get"
    (h/with-system [sut (core/todo-service-system config)]
                   (let [url (h/url-for :list-view :path-params {:list-id test-list-id})
                         service (h/service-fn sut)
                         {:keys [status body]} (response-for service :get url)]
                     (is (= 200 status))
                     (is (= (str test-list) body))))))

(comment
  (run-tests)
  )