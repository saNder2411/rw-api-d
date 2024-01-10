(ns td-service.system.system-test
  (:require [clojure.test :refer [deftest testing is run-tests]]
            [io.pedestal.test :refer [response-for]]
            [utils.helpers :as h]
            [td-service.core :as core]))

(def sut (gensym "sut"))

(def test-list-id (atom ""))

(def test-item-id (atom ""))

(def config {:env     :test
             :server  {:port 3001}
             :htmx    {:server {:port 3002}}
             :db-spec {:jdbcUrl  "jdbc:postgresql://localhost:5432/rwa"
                       :username "rwa"
                       :password "rwa"}})

(deftest system-test
  (h/with-system [sut (core/todo-service-system config)]
                 (let [service (h/service-fn sut)]
                   (testing "/todo :post"
                     (let [url (h/url-for :list-create :query-params {:title "_TEST_"})
                           {:keys [status body]} (response-for service :post url :headers {"Accept" "application/edn"})
                           body-clj-map (read-string body)]
                       (swap! test-list-id (fn [_] (:id body-clj-map)))
                       (is (= 201 status))
                       (is (= "_TEST_" (:title body-clj-map)))))

                   (testing "/todo :get"
                     (let [url (h/url-for :lists-view)
                           {:keys [status body]} (response-for service :get url :headers {"Accept" "application/edn"})
                           body-clj-map (read-string body)]
                       (is (= 200 status))
                       (is (contains? body-clj-map @test-list-id))))

                   (testing "/todo/:list-id :get"
                     (let [url (h/url-for :list-view :path-params {:list-id @test-list-id})
                           {:keys [status body]} (response-for service :get url :headers {"Accept" "application/edn"})
                           body-clj-map (read-string body)]
                       (is (= 200 status))
                       (is (= @test-list-id (:id body-clj-map)))
                       (is (= "_TEST_" (:title body-clj-map)))))

                   (testing "/todo/:list-id :put"
                     (let [url (h/url-for :list-update :path-params {:list-id @test-list-id} :query-params {:title "_TEST_!"})
                           {:keys [status body]} (response-for service :put url :headers {"Accept" "application/edn"})
                           body-clj-map (read-string body)]
                       (is (= 200 status))
                       (is (= @test-list-id (:id body-clj-map)))
                       (is (= "_TEST_!" (:title body-clj-map)))))

                   (testing "/todo/:list-id :post"
                     (let [url (h/url-for :list-item-create :path-params {:list-id @test-list-id} :query-params {:title "_TEST_ITEM_"})
                           {:keys [status body]} (response-for service :post url :headers {"Accept" "application/edn"})
                           body-clj-map (read-string body)]
                       (swap! test-item-id (fn [_] (:id body-clj-map)))
                       (is (= 201 status))
                       (is (= "_TEST_ITEM_" (:title body-clj-map)))))

                   (testing "/todo/:list-id/:item-id :get"
                     (let [url (h/url-for :list-item-view :path-params {:list-id @test-list-id :item-id @test-item-id})
                           {:keys [status body]} (response-for service :get url :headers {"Accept" "application/edn"})
                           body-clj-map (read-string body)]
                       (is (= 200 status))
                       (is (= @test-item-id (:id body-clj-map)))
                       (is (= "_TEST_ITEM_" (:title body-clj-map)))))

                   (testing "/todo/:list-id/:item-id :put"
                     (let [url (h/url-for
                                 :list-item-update
                                 :path-params {:list-id @test-list-id :item-id @test-item-id}
                                 :query-params {:title "_TEST_ITEM_!"})
                           {:keys [status body]} (response-for service :put url :headers {"Accept" "application/edn"})
                           body-clj-map (read-string body)]
                       (is (= 200 status))
                       (is (= @test-item-id (:id body-clj-map)))
                       (is (= "_TEST_ITEM_!" (:title body-clj-map)))))

                   (testing "/todo/:list-id/:item-id :delete"
                     (let [url (h/url-for :list-item-delete :path-params {:list-id @test-list-id :item-id @test-item-id})
                           {:keys [status body]} (response-for service :delete url :headers {"Accept" "application/edn"})
                           body-clj-map (read-string body)]
                       (is (= 200 status))
                       (is (not (contains? (:items body-clj-map) @test-item-id)))))

                   (testing "/todo/:list-id :delete"
                     (let [url (h/url-for :list-delete :path-params {:list-id @test-list-id})
                           {:keys [status body]} (response-for service :delete url :headers {"Accept" "application/edn"})
                           body-clj-map (read-string body)]
                       (is (= 200 status))
                       (is (not (contains? body-clj-map @test-list-id))))))))


(comment
  (run-tests)
  )