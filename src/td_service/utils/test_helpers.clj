(ns td-service.utils.test-helpers
  (:require [com.stuartsierra.component :as component]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [clojure.string :as string])
  (:import (java.net ServerSocket)))

(defn url-for-routes [system server-name]
  (route/url-for-routes (get-in system [server-name :server ::http/routes])))

(defn service-fn [system] (get-in system [:pedestal :server ::http/service-fn]))

(defmacro with-system [[bound-var binding-exp] & body]
  `(let [~bound-var (component/start ~binding-exp)]
     (try
       ~@body
       (finally
         (component/stop ~bound-var)))))


(defn sut->url [sut path]
  (string/join ["http://localhost:" (-> sut :pedestal-comp :config :server :port) path]))

(defn get-free-port []
  (with-open [socket (ServerSocket. 0)]
    (.getLocalPort socket)))
