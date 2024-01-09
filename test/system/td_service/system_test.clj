(ns system.td-service.system-test
  (:require [clojure.test :refer [deftest testing is run-tests]]
            [io.pedestal.test :refer [response-for]]
            [helpers.common :as h]
            [td-service.core :as core]))

(def sut (gensym "sut"))

(def config {:env     :test
             :server  {:port 3001}
             :htmx    {:server {:port 3002}}
             :db-spec {:jdbcUrl  "jdbc:postgresql://localhost:5432/rwa"
                       :username "rwa"
                       :password "rwa"}})

(deftest system-test
  (testing "/greet :get test"
    (h/with-system [sut (core/todo-service-system config)]
                   (let [service (h/service-fn sut)
                         {:keys [status body]} (response-for service :get (h/url-for :greet))]
                     (is (= 200 status))
                     (is (= "Hello, World!" body)))))

  (testing "/todo :get test"
    (h/with-system [sut (core/todo-service-system config)]
                   (let [service (h/service-fn sut)
                         {:keys [status body]} (response-for service :get (h/url-for :lists-view))]
                     (is (= 200 status))
                     (is (= "{}" body))))))



(comment
  (run-tests)
  )