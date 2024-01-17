(ns htmx.routes.tw-classes
  (:require [clojure.string :as string]))


(defn tw->str [classes]
  (->> (flatten classes)
       (remove nil?)
       (map name)
       (sort)
       (string/join " ")))

(def primary-button
  [:bg-blue-500 :hover:bg-blue-400 :text-white :font-bold :py-2 :px-4 :border-b-4 :border-blue-700 :hover:border-blue-500 :rounded])

(def cancel-button
  [:bg-red-500 :hover:bg-red-400 :text-white :font-bold :py-2 :px-4 :border-b-4 :border-red-700 :hover:border-red-500 :rounded])

(def input-c
  [:bg-gray-200 :appearance-none :border-2 :border-gray-200 :rounded :py-2 :px-4 :text-gray-700 :leading-tight :focus:outline-none :focus:bg-white :focus:border-blue-500])