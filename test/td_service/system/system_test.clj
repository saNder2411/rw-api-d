(ns td-service.system.system-test
  (:require [clojure.test :refer [deftest testing is run-tests]]
            [io.pedestal.test :refer [response-for]]
            [clojure.data.json :as json]
            [core :as core]
            [td-service.utils.test-helpers :as h]))

(def sut (gensym "sut"))

(def test-list-id (atom ""))

(def test-item-id (atom ""))

(def config {:td-service {:env     :test
                          :server  {:port 3001}
                          :db-spec {:jdbcUrl  "jdbc:postgresql://localhost:5432/rwa"
                                    :username "rwa"
                                    :password "rwa"}}})

(deftest system-test
  (let [db-container (h/create-db-container)]
    (try
      (.start db-container)
      (h/with-system [sut (core/services-system (assoc-in config [:td-service :db-spec] {:jdbcUrl  (.getJdbcUrl db-container)
                                                                                         :username (.getUsername db-container)
                                                                                         :password (.getPassword db-container)}))]
                     (let [service (h/service-fn sut :td-pedestal)
                           url-for (h/url-for-routes sut :td-pedestal)]
                       (testing "/todos :post"
                         (let [url (url-for :list-create :query-params {:title "_TEST_"})
                               {:keys [status body]} (response-for service :post url :headers {"Accept" "application/edn"})
                               body-clj-map (read-string body)]
                           (println "CREATE_RES:" body-clj-map)
                           (swap! test-list-id (fn [_] (str (:id body-clj-map))))
                           (is (= 201 status))
                           (is (= "_TEST_" (:title body-clj-map)))))

                       (testing "/todo :get"
                         (let [url (url-for :lists-view)
                               {:keys [status body]} (response-for service :get url :headers {"Accept" "application/edn"})
                               body-clj-map (read-string body)]
                           (println "GET_ALL_RES:" body-clj-map)
                           (is (= 200 status))
                           (is (some #(= (str (:id %)) @test-list-id) body-clj-map))))

                       (testing "/todo/:list-id :get"
                         (let [url (url-for :list-view :path-params {:list-id @test-list-id})
                               {:keys [status body]} (response-for service :get url :headers {"Accept" "application/edn"})
                               body-clj-map (read-string body)]
                           (println "GET_RES:" body-clj-map)
                           (is (= 200 status))
                           (is (= @test-list-id (str (:id body-clj-map))))
                           (is (= "_TEST_" (:title body-clj-map)))))

                       (testing "/todo/:list-id :put"
                         (let [url (url-for :list-update :path-params {:list-id @test-list-id} :query-params {:title "_TEST_!"})
                               {:keys [status body]} (response-for service :put url :headers {"Accept" "application/edn"})
                               body-clj-map (read-string body)]
                           (println "PUT_RES:" body-clj-map)
                           (is (= 200 status))
                           (is (= @test-list-id (str (:id body-clj-map))))
                           (is (= "_TEST_!" (:title body-clj-map)))))

                       (testing "/todo/:list-id :post"
                         (let [url (url-for :list-item-create :path-params {:list-id @test-list-id})
                               headers {"Accept"       "application/edn"
                                        "Content-Type" "application/json"}
                               body (json/write-str {:title "_TEST_ITEM_" :done false})
                               {:keys [status body]} (response-for service :post url :headers headers :body body)
                               body-clj-map (read-string body)]
                           (println "ITEM_POST_RES:" body-clj-map)
                           (swap! test-item-id (fn [_] (str (:id body-clj-map))))
                           (is (= 201 status))
                           (is (= "_TEST_ITEM_" (:title body-clj-map)))))

                       (testing "/todo/:list-id/:item-id :get"
                         (let [url (url-for :list-item-view :path-params {:list-id @test-list-id :item-id @test-item-id})
                               {:keys [status body]} (response-for service :get url :headers {"Accept" "application/edn"})
                               body-clj-map (read-string body)]
                           (println "ITEM_GET_RES:" body-clj-map)
                           (is (= 200 status))
                           (is (= @test-item-id (str (:id body-clj-map))))
                           (is (= "_TEST_ITEM_" (:title body-clj-map)))))

                       (testing "/todo/:list-id/:item-id :put"
                         (let [url (url-for
                                     :list-item-update
                                     :path-params {:list-id @test-list-id :item-id @test-item-id}
                                     :query-params {:title "_TEST_ITEM_!"})
                               {:keys [status body]} (response-for service :put url :headers {"Accept" "application/edn"})
                               body-clj-map (read-string body)]
                           (println "ITEM_PUT_RES:" body-clj-map)
                           (is (= 200 status))
                           (is (= @test-item-id (str (:id body-clj-map))))
                           (is (= "_TEST_ITEM_!" (:title body-clj-map)))))

                       (testing "/todo/:list-id/:item-id :delete"
                         (let [url (url-for :list-item-delete :path-params {:list-id @test-list-id :item-id @test-item-id})
                               {:keys [status body]} (response-for service :delete url :headers {"Accept" "application/edn"})
                               body-clj-map (read-string body)]
                           (println "ITEM_DEL_RES:" body-clj-map)
                           (is (= 200 status))
                           (is (not (some #(= (str (:id %)) @test-list-id) body-clj-map)))))

                       (testing "/todo/:list-id :delete"
                         (let [url (url-for :list-delete :path-params {:list-id @test-list-id})
                               {:keys [status body]} (response-for service :delete url :headers {"Accept" "application/edn"})
                               body-clj-map (read-string body)]
                           (println "DEL_RES:" body-clj-map)
                           (is (= 200 status))
                           (is (not (some #(= (str (:id %)) @test-list-id) body-clj-map)))))))
      (finally
        (.stop db-container)))))


(comment
  (run-tests)
  )