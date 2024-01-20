(ns td-service.db.postgresql
  (:require [honey.sql :as sql]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]))


(defn create-el [data-source table values]
  (let [query (sql/format {:insert-into [table]
                           :values      values
                           :returning   :*})]
    (jdbc/execute-one! (data-source) query {:builder-fn rs/as-unqualified-kebab-maps})))

(defn find-by-id [data-source table id]
  (let [query (sql/format {:select :*
                           :from   table
                           :where  [:= :_id (parse-uuid id)]})]
    (jdbc/execute-one! (data-source) query {:builder-fn rs/as-unqualified-kebab-maps})))


(defn find-all [data-source table]
  (let [query (sql/format {:select :*
                           :from   table})]
    (jdbc/execute! (data-source) query {:builder-fn rs/as-unqualified-kebab-maps})))

(defn update-el [data-source table value id]
  (let [query (sql/format {:update table
                           :set    value
                           :where  [:= :_id (parse-uuid id)]
                           :returning   :*})]
    (jdbc/execute-one! (data-source) query {:builder-fn rs/as-unqualified-kebab-maps})))


(defn delete-el [data-source table id]
  (let [delete-query (sql/format {:delete-from [table]
                           :where       [:= :_id (parse-uuid id)]
                           :returning   :*})
        select-query (sql/format {:select :*
                           :from   table})]
    (jdbc/execute! (data-source) delete-query {:builder-fn rs/as-unqualified-kebab-maps})
    (jdbc/execute! (data-source) select-query {:builder-fn rs/as-unqualified-kebab-maps})))



;;;
;;; Domain functions
;;;
(defn make-list [title] {:title title})

(defn make-item [l-id item] (into {:list-id (parse-uuid l-id)} item))