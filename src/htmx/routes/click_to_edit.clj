(ns htmx.routes.click-to-edit
  (:require [io.pedestal.http.body-params :as body-params]
            [htmx.routes.shared :as s]
            [htmx.routes.tw-classes :as tw]))


(defn- dependencies->state [dependencies]
  (get-in dependencies [:htmx-in-memory-db :htmx-click-to-edit-state]))

(defn user-details-component [{:keys [id first-name last-name email]}]
  [:div {:class     (tw/tw->str [:p-5 :bg-slate-100])
         :hx-target "this"
         :hx-swap   "outerHTML"}
   [:div {:class "mt-2 flex gap-2 items-center"}
    [:label "First Name:"]
    [:span {:class (tw/tw->str [:font-bold])} first-name]]
   [:div {:class "mt-2 flex gap-2 items-center"}
    [:label "Last Name:"]
    [:span {:class (tw/tw->str [:font-bold])} last-name]]
   [:div {:class "mt-2 flex gap-2 items-center"}
    [:label "Email:"]
    [:span {:class (tw/tw->str [:font-bold])} email]]
   [:button {:class  (tw/tw->str [tw/primary-button :mt-5])
             :hx-get (format "/htmx/click-to-edit/user/%s/edit" id)}
    "Click To Edit"]])

(defn click-to-edit-form [{:keys [id first-name last-name email]}]
  [:form
   {:class     (tw/tw->str [:bg-slate-100
                            :p-5])
    :hx-put    (format "/htmx/click-to-edit/user/%s/edit" id)
    :hx-target "this"
    :hx-swap   "outerHTML"}
   [:div {:class "mt-2 flex gap-2 items-center"}
    [:label "First Name"]
    [:input {:class (tw/tw->str [tw/input-c])
             :type  "text"
             :name  "first-name"
             :value first-name}]]
   [:div {:class "mt-2 flex gap-2 items-center"}
    [:label "Last Name"]
    [:input {:class (tw/tw->str [tw/input-c])
             :type  "text"
             :name  "last-name"
             :value last-name}]]
   [:div {:class "mt-2 flex gap-2 items-center"}
    [:label "Email"]
    [:input {:class (tw/tw->str [tw/input-c])
             :type  "email"
             :name  "email"
             :value email}]]
   [:div {:class (tw/tw->str [:flex :flex-row :gap-2 :mt-5])}
    [:button {:class (tw/tw->str [tw/primary-button])} "Submit"]
    [:button {:hx-get    "/htmx/click-to-edit"
              :hx-target "body"
              :class     (tw/tw->str [tw/cancel-button])} "Cancel"]]])

(def layout (partial s/layout
                     "Click to edit"
                     ["https://cdn.tailwindcss.com" "https://unpkg.com/htmx.org@1.9.4?plugins=forms"]))

;; Pedestal handlers
(def root-handler {:name  ::root
                   :enter (fn [{:keys [dependencies] :as context}]
                            (let [users @(dependencies->state dependencies)
                                  response
                                  (-> [:div
                                       (map (fn [[id user]]
                                              [:div {:class (tw/tw->str [:mt-2])}
                                               (user-details-component (assoc user :id id))]) users)]
                                      (layout)
                                      (s/ok))]
                              (assoc context :response response)))})

(def get-form-handler {:name  ::get
                       :enter (fn [{:keys [dependencies] :as context}]
                                (let [user-id (-> context :request :path-params :user-id)
                                      users @(dependencies->state dependencies)
                                      user (get users user-id)
                                      response (-> (assoc user :id user-id)
                                                   (click-to-edit-form)
                                                   (s/ok))]
                                  (assoc context :response response)))})

(def put-form-handler {:name  ::put
                       :enter (fn [{:keys [dependencies request] :as context}]
                                (let [user-id (-> context :request :path-params :user-id)
                                      _ (swap! (dependencies->state dependencies)
                                               assoc user-id (:form-params request))
                                      users @(dependencies->state dependencies)
                                      user (get users user-id)
                                      response (-> (assoc user :id user-id)
                                                   (user-details-component)
                                                   (s/ok))]
                                  (assoc context :response response)))})

(def routes #{["/htmx/click-to-edit" :get [root-handler]]
              ["/htmx/click-to-edit/user/:user-id/edit" :get [get-form-handler]]
              ["/htmx/click-to-edit/user/:user-id/edit" :put [(body-params/body-params) put-form-handler]]})