(ns config
  (:require [aero.core :as aero]
            [clojure.java.io :as io]
            [malli.core :as m]
            [malli.error :as me]
            [malli.util :as mu]))




(def ServerConfigSchema
  (m/schema
    [:map
     [:port [:int {:min 1 :max 9999}]]]))

(def DBSpecSchema
  (m/schema
    [:map
     [:jdbcUrl :string]
     [:username :string]
     [:password :string]]))

(def SystemConfigSchema
  (m/schema
    [:map
     [:td-service [:map
                   [:server ServerConfigSchema]
                   [:env :keyword]
                   [:db-spec DBSpecSchema]]]
     [:htmx [:map
             [:server ServerConfigSchema]
             [:env :keyword]]]]))

(defn assert-valid-config! [config]
  (if (m/validate SystemConfigSchema config)
    config
    (->> {:error (me/humanize (m/explain SystemConfigSchema config))}
         (ex-info "Config is not valid!")
         (throw))))

(defn read-config []
  (-> "config.edn" io/resource aero/read-config assert-valid-config!))

(def KafkaConfigBaseSchema
  (m/schema
    [:map
     [:bootstrap-servers :string]
     [:application-id :string]
     [:auto-offset-reset [:enum "earliest" "latest"]]
     [:producer-asks [:enum "0" "1" "all"]]]))

(def KafkaConfigSchema
  (m/schema
    [:multi {:dispatch :security-protocol}
     ["SSL"
      (mu/merge
        KafkaConfigBaseSchema
        [:map
         [:security-protocol [:enum "SSL"]]
         [:ssl-keystore-type [:enum "PKCS12"]]
         [:ssl-truststore-type [:enum "JKS"]]
         [:ssl-keystore-location :string]
         [:ssl-keystore-password :string]
         [:ssl-key-password :string]
         [:ssl-truststore-location :string]
         [:ssl-truststore-password :string]])]
     ["PLAINTEXT"
      (mu/merge
        KafkaConfigBaseSchema
        [:map
         [:security-protocol [:enum "PLAINTEXT"]]])]]))


(comment
  (-> ServerConfigSchema
      (m/validate {:port 9090}))

  (-> DBSpecSchema
      (m/validate {:jdbcUrl  "jdbc:postgresql://localhost:5432/rwa"
                   :username "rwa"
                   :password "rwa"}))

  (-> SystemConfigSchema
      (m/validate (read-config)))


  (-> [:map {:closed true}
       [:id :string]
       [:count :int]
       [:some-optional-key {:optional true} [:maybe :keyword]]]
      (m/explain {:id "id" :count 1 :some-optional-key nil})
      (me/humanize))

  (-> KafkaConfigSchema
      (m/explain {:security-protocol       "SSL"
                  :bootstrap-servers       "kafka",
                  :application-id          "my-app",
                  :auto-offset-reset       "latest",
                  :producer-asks           "0"
                  :ssl-keystore-type       "PKCS12",
                  :ssl-truststore-type     "JKS",
                  :ssl-keystore-location   "loc",
                  :ssl-keystore-password   "pwd",
                  :ssl-key-password        "pwd",
                  :ssl-truststore-location "loc",
                  :ssl-truststore-password "pwd"})
      (me/humanize))
  (assert-valid-config! (dissoc (read-config) :htmx))
  )