(ns td-service.db.atom-db)

;;;
;;; Atom DB
;;;
(defonce atom-db (atom {:list-collection {}
                        :item-collection {}}))

(defn update-element [db-val collection-key id new-state]
  (if (contains? (get db-val collection-key) id)
    (update-in db-val [collection-key id] into new-state)
    db-val))

(defn delete-element [db-val collection-key id]
  (if (contains? (get db-val collection-key) id)
    (let [collection (get db-val collection-key)]
      (assoc db-val collection-key (dissoc collection id)))
    db-val))

(defn create-item-el [db-val l-id i-id new-item]
  (if (contains? (get db-val :list-collection) l-id)
    (-> db-val
        (assoc-in [:item-collection i-id] new-item)
        (update-in [:list-collection l-id :item-ids] conj i-id))
    db-val))

(defn delete-item-el [db-val l-id i-id]
  (if (contains? (get db-val :list-collection) l-id)
    (let [item-coll (get db-val :item-collection)
          n-item-coll (dissoc item-coll i-id)]
      (-> db-val
          (update-in [:list-collection l-id :item-ids] (fn [ids] (filter #(not (= i-id %)) ids)))
          (assoc :item-collection n-item-coll)))
    db-val))

;;;
;;; Domain functions
;;;
(defn make-list [id title] {:_id id :title title :item-ids []})

(defn make-item [id l-id item] (into {:_id id :list-id l-id} item))