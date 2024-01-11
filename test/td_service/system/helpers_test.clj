(ns td-service.system.helpers-test
  (:require [com.stuartsierra.component :as component]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [td-service.components.pedestal.routes :as routes]))

(def url-for (route/url-for-routes routes/routes))

(defn service-fn [system] (get-in system [:pedestal :server ::http/service-fn]))

(defmacro with-system [[bound-var binding-exp] & body]
  `(let [~bound-var (component/start ~binding-exp)]
     (try
       ~@body
       (finally
         (component/stop ~bound-var)))))
