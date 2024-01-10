(ns td-service.db.atom-db)
;;;
;;; Domain functions
;;;
(defn make-list [title id] {:id id :title title :items {}})

(defn make-list-item [title l-id id] {:id id :list-id l-id :title title :done? false})
;;;
;;; Test list
;;;
(def test-list-id (str "l-" (random-uuid)))
(def test-item-id (str test-list-id "-i-" (random-uuid)))

(def test-list (let [list (make-list "Test List" test-list-id)
                     list-item (make-list-item "Test Item" test-list-id test-item-id)]
                 (assoc list :items {test-item-id list-item})))
;;;
;;; Atom DB
;;;
(defonce atom-db (atom {test-list-id test-list}))

(defn get-list-by-id [db-val id] (get db-val id))

(defn get-list-item-by-id [db-val l-id i-id] (get-in db-val [l-id :items i-id] nil))

(defn delete-list [db-val id]
  (if (contains? db-val id)
    (dissoc db-val id)
    db-val))

(defn update-list [db-val id new-state]
  (if (contains? db-val id)
    (let [list (get db-val id)
          new-list (into list new-state)]
      (assoc db-val id new-list))
    db-val))

(defn create-list-item [db-val l-id i-id new-item]
  (if (contains? db-val l-id)
    (assoc-in db-val [l-id :items i-id] new-item)
    db-val))

(defn delete-list-item [db-val l-id i-id]
  (if (contains? db-val l-id)
    (update-in db-val [l-id :items] dissoc i-id)
    db-val))

(defn update-list-item [db-val l-id i-id new-state]
  (if (contains? db-val l-id)
    (let [item (get-in db-val [l-id :items i-id])]
      (update-in db-val [l-id :items i-id] merge item new-state))
    db-val))
