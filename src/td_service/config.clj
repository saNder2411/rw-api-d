(ns td-service.config
  (:require [aero.core :as aero]
            [clojure.java.io :as io]))

(defn read-config []
  (-> "config.edn" io/resource aero/read-config))
