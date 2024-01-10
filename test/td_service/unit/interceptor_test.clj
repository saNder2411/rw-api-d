(ns td-service.unit.interceptor-test
  (:require [clojure.test :refer [deftest testing is run-tests]]
            [io.pedestal.interceptor.chain :as chain]
            [td-service.components.pedestal.routes :as r]))

(deftest interceptor-test
  #_(testing "db-interceptor-test"
      (is (= 1 (:database (chain/execute {} [(i/interceptor r/db-interceptor)])))))

  (testing "entity-render-test"
    (let [ctx {:result {:body {:title "A-List" :items {}}}}]
      (is (=
            {:status 200, :body {:title "A-List", :items {}}, :headers nil}
            (:response (chain/execute ctx [r/entity-render])))))

    (let [ctx {:result {:created? true :body {:title "A-List" :items {}}}}]
      (is (=
            {:status 201, :body {:title "A-List", :items {}}, :headers nil}
            (:response (chain/execute ctx [r/entity-render]))))))

  (testing "lists-view"
    (let [ctx {:request {:database {}}}]
      (is (=
            {:body {}}
            (:result (chain/execute ctx [r/lists-view]))))))

  (testing "list-view"
    (let [ctx {:request {:path-params {:list-id 1} :database {1 {:title "A-List" :items {}}}}}]
      (is (=
            {:body {:title "A-List" :items {}}}
            (:result (chain/execute ctx [r/list-view])))))))


(comment
  (interceptor-test)
  (run-tests 'unit.td-service.interceptor-test)
  )
