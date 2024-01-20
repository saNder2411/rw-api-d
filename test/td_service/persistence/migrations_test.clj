(ns td-service.persistence.migrations-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [td-service.utils.test-helpers :as h]
            [honey.sql :as sql]))

(def sut (gensym "sut"))

(deftest migrations-honeysql-query-test
  (let [db-container (h/create-db-container)]
    (try
      (.start db-container)
      (h/with-system
        [sut (h/datasource-only-system {:db-spec {:jdbcUrl  (.getJdbcUrl db-container)
                                                  :username (.getUsername db-container)
                                                  :password (.getPassword db-container)}})]
        (testing "select * from schema_version"
          (let [{:keys [data-source]} sut
                select-query (sql/format {:select :*
                                          :from   :schema-version})
                [schema-version :as schema-versions] (jdbc/execute!
                                                       (data-source)
                                                       select-query
                                                       {:builder-fn rs/as-unqualified-lower-maps})]

            (is (= ["SELECT * FROM schema_version"] select-query))
            (is (= 1 (count schema-versions)))
            (is (= {:description "add todo tables"
                    :script      "V1__add_todo_tables.sql"
                    :success     true}
                   (select-keys schema-version [:description :script :success])))))

        (testing "insert list-table-test"
          (let [{:keys [data-source]} sut
                insert-query (sql/format {:insert-into [:lists]
                                          :columns     [:title]
                                          :values      [["My Todo List"] ["Other Todo List"]]
                                          :returning   :*})

                insert-results (jdbc/execute! (data-source) insert-query {:builder-fn rs/as-unqualified-lower-maps})

                select-results (jdbc/execute!
                                 (data-source)
                                 (sql/format {:select :* :from :lists})
                                 {:builder-fn rs/as-unqualified-lower-maps})]
            (is (= ["INSERT INTO lists (title) VALUES (?), (?) RETURNING *" "My Todo List" "Other Todo List"] insert-query))
            (is (= 2 (count insert-results) (count select-results)))
            (is (= #{"My Todo List" "Other Todo List"}
                   (->> insert-results (map :title) (into #{}))
                   (->> select-results (map :title) (into #{})))))))
      (finally
        (.stop db-container)))))

(comment
  (run-tests)
  )
