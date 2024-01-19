(ns htmx.routes.infinite-scroll
  (:require [faker.lorem :as fl]
            [faker.name :as fn]
            [htmx.routes.shared :as shared]))

(def page-size 10)

(defn random-infinite-scroll-item []
  {:title       (first (fl/sentences))
   :description (first (fl/paragraphs))
   :author      {:name    (first (fn/names))
                 :picture (shared/random-picture)}})

(def items (repeatedly 50 random-infinite-scroll-item))

(defn items->page [page-number]
  (nth (partition-all page-size items) page-number nil))

;; Hiccup components
(defn item-component [{:keys [id title description author]}]
  [:article.p-6.even:bg-white.odd:bg-slate-100.sm:p-8
   [:h2.break-all.text-lg.font-medium.sm:text-xl
    [:a.hover:underline
     {:href (str "htmx/infinite-scroll/item/" id)}
     title]]
   [:p.mt-1.break-all.text-sm.text-gray-700
    description]
   [:div.mt-4.text-xs.font-medium.text-gray-500
    [:div.flex.items-center.gap-2
     [:span.relative.flex.h-10.w-10.shrink-0.overflow-hidden.rounded-full
      [:img.aspect-square.h-full.w-full
       {:src (:picture author "https://avataaars.io/?hairColor=BrownDark")}]]
     [:span (:name author)]]]])

(defn loader [next-page-number]
  [:div.bg-red-100.p-10
   {:hx-get     (str "/htmx/infinite-scroll/items?page=" next-page-number)
    :hx-trigger "revealed"
    :hx-target  "this"
    :hx-swap    "outerHTML"}
   [:span (format "...loading page %s ..." next-page-number)]])

(def layout (partial shared/layout "Infinite scroll"
                     ["https://cdn.tailwindcss.com"
                      "https://unpkg.com/htmx.org@1.9.4?plugins=forms"]))

(def root-handler {:name  ::root
                   :enter (fn [ctx]
                            (let [initial-page-number 0
                                  items-page (items->page initial-page-number)
                                  response (-> [:div.shadow-xl
                                                (map item-component items-page)
                                                (loader (inc initial-page-number))]
                                               (layout)
                                               (shared/ok))]
                              (assoc ctx :response response)))})


(def get-items-page-handler {:name ::get
                             :enter
                             (fn [ctx]
                               (let [page-number (-> ctx :request :query-params :page parse-long)
                                     items-page (items->page page-number)
                                     response (if (seq items-page)
                                                (-> (mapv item-component items-page)
                                                    (conj (loader (inc page-number)))
                                                    (seq)
                                                    (shared/ok))
                                                (-> [:div.bg-green-100.p-10
                                                     [:span "nothing to load"]]
                                                    (shared/ok)))]
                                 (assoc ctx :response response)))})

(def routes
  #{["/htmx/infinite-scroll" :get [root-handler]]
    ["/htmx/infinite-scroll/items" :get [get-items-page-handler]]})