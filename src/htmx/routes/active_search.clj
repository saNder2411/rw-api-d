(ns htmx.routes.active-search
  (:require [clojure.string :as string]
            [htmx.routes.shared :as s]
            [htmx.routes.tw-classes :as tw]))

(defn load-text-data* []
  (let [lines (-> (slurp "https://ocw.mit.edu/ans7870/6/6.006/s08/lecturenotes/files/t8.shakespeare.txt")
                  (string/split #"\n"))]
    (->> lines
         (remove string/blank?)
         (map string/trim))))

(def load-text-data (memoize load-text-data*))

(def layout (partial s/layout
                     "HTMX: Active Search"
                     ["https://cdn.tailwindcss.com" "https://unpkg.com/htmx.org@1.9.4?plugins=forms"]))

(def root-handler {:name  ::root
                   :enter (fn [ctx]
                            (assoc ctx :response (->> (list
                                                        [:div.htmx-indicator "Searching..."]
                                                        [:input
                                                         {:class        (tw/tw->str [tw/input-c])
                                                          :type         "search"
                                                          :name         "q"
                                                          :placeholder  "Begin Typing To Search..."
                                                          :hx-get       "/htmx/active-search/search"
                                                          :hx-trigger   "keyup changed delay:500ms"
                                                          :hx-target    "#search-results"
                                                          :hx-indicator ".htmx-indicator"}]
                                                        [:div#search-results "Search results"])
                                                      (layout)
                                                      (s/ok))))})

(def search-handler {:name  ::search
                     :enter (fn [ctx]
                              (let [q (-> ctx :request :query-params :q string/trim)
                                    lines (if (string/blank? q)
                                            []
                                            (->> (load-text-data)
                                                 (filter #(string/includes? % q))
                                                 (take 50)))
                                    response (-> (map (fn [line]
                                                        [:div.even:bg-red-50.odd:bg-green-50.p-2 line]) lines)
                                                 (doall)
                                                 (s/ok))]
                                (assoc ctx :response response)))})

(def routes
  #{["/htmx/active-search" :get [root-handler]]
    ["/htmx/active-search/search" :get [search-handler]]})
