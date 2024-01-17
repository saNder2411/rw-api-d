(ns htmx.routes.comments-section
  (:require [faker.lorem :as fl]
            [faker.name :as fn]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.params :as params]
            [htmx.routes.shared :as s]
            [htmx.routes.tw-classes :as tw]))

(defn random-comment []
  {:name    (first (fn/names))
   :comment (first (fl/paragraphs))
   :picture (s/random-picture)})

(def comments-atom (atom (repeatedly 3 random-comment)))

(def author-picture (s/random-picture))
(def author-name "Andrey Fadeev")

(defn comment-component
  ([comment] (comment-component comment {}))
  ([comment opts]
   [:div.odd:bg-white.even:bg-slate-100.px-4.py-4.sm:px-6.lg:px-8
    opts
    [:p.text-gray-700.text-sm.linkify.break-all
     (:comment comment)]
    [:div.mt-2.flex.items-center.gap-1.text-xs.text-gray-500
     [:a.flex.items-center.hover:underline.gap-1
      {:href "#"}
      [:img.rounded-full.h-5.w-5
       {:src (:picture comment)}]
      [:span (:name comment)]]]]))

(defn comment-form-component []
  [:form.px-4.py-4.sm:px-6.lg:px-8
   {:hx-post   "/htmx/comments-section/comments"
    :hx-target "this"
    :hx-swap   "outerHTML"
    :hx-boost  "true"
    }
   [:div
    [:label.sr-only {:for "comment"} "Comment"]
    [:textarea#comment.w-full.p-3.text-sm.rounded-sm
     {:autocomplete "off"
      :name         "comment"
      :class        (tw/tw->str [tw/input-c])
      :placeholder  "Your comment" :rows "5"}]]
   [:div.mt-2
    [:button.px-5.py-3.text-white.text-sm.bg-indigo-800.rounded-sm
     {:type "submit"}
     [:span.font-medium "Publish comment"]]]])

(defn comments-component [comments]
  [:div#comments
   {:hx-get     "/htmx/comments-section/comments"
    :hx-trigger "newComment from:body"}
   [:h2.px-4.sm:px-6.lg:px-8.bg-gray-100.py-5.text-lg.font-medium.sm:text-xl
    (format "Comments (%s)" (count comments))]
   [:div.mx-auto
    (for [comment comments]
      (comment-component comment))]])

(defn comments-section-component [comments]
  [:div#comments-section.shadow-xl.mt-4
   (comments-component comments)
   (comment-form-component)])

(def layout (partial s/layout
                     "HTMX: Comments section"
                     ["https://cdn.tailwindcss.com" "https://unpkg.com/htmx.org@1.9.4?plugins=forms"]))

(def root-handler {:name ::root
                   :enter
                   (fn [context]
                     (let [response
                           (-> (comments-section-component @comments-atom)
                               (layout)
                               (s/ok))]
                       (assoc context :response response)))})

(def post-handler {:name ::post
                   :enter
                   (fn [context]
                     (let [comment {:comment (-> context :request :params :comment)
                                    :name    author-name
                                    :picture author-picture}
                           _ (swap! comments-atom conj comment)
                           response (-> (comment-form-component)
                                        (s/ok))]
                       (assoc context :response
                                      (update response :headers merge {"HX-Trigger" "newComment"}))))})

(def comments-handler {:name ::comments
                       :enter
                       (fn [context]
                         (let [response
                               (-> (comments-component @comments-atom)
                                   (s/ok))]
                           (assoc context :response response)))})

(def routes
  #{["/htmx/comments-section" :get [root-handler]]
    ["/htmx/comments-section/comments" :get comments-handler]
    ["/htmx/comments-section/comments" :post [(body-params/body-params) params/keyword-params post-handler]]})


(comment
  {:hx-post   "/htmx/comments-section/comments"
   :hx-target "#comments-section"
   :hx-swap   "outerHTML"
   :hx-boost  "true"
   }


  {:hx-swap-oob "afterbegin:#comments"}

  (update {} :headers merge {"HX-Trigger" "newComment"})

  ["/htmx/comments-section/comments"
   :get comments-handler
   :route-name ::comments]

  {:hx-get     "/contacts/table"
   :hx-trigger "newComment from:body, every 10s"})