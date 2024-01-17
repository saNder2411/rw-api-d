(ns htmx.db.atom-db)
;;;
;;; Atom DB
;;;
(defonce atom-db (atom {"1" {:first-name "changeit"
                             :last-name  "changeit"
                             :email      "change@it.com"}
                        "2" {:first-name "user2"
                             :last-name  "user2"
                             :email      "change@it.com"}
                        "3" {:first-name "user3"
                             :last-name  "user3"
                             :email      "change@it.com"}}))
