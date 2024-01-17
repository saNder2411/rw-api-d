(ns htmx.routes.delete-with-confirmation
  (:require [faker.lorem :as fl]
            [htmx.routes.shared :as s]))

(defn random-item []
  {:id    (random-uuid)
   :title (first (fl/sentences))})

(def items-atom (atom (repeatedly 50 random-item)))

(defn item-div-id [item]
  (str "item-div-" (:id item)))

(defn item-div-id-ref [item]
  (str "#" (item-div-id item)))

(def layout (partial s/layout "HTMX: Delete with confirmation"
                     ["https://cdn.tailwindcss.com?plugins=forms"
                      "https://unpkg.com/htmx.org@1.9.4"
                      "https://unpkg.com/hyperscript.org@0.9.11"
                      "https://cdn.jsdelivr.net/npm/sweetalert2@11"]))

(def root-handler {:name ::root
                   :enter
                   (fn [ctx]
                     (let [response (-> [:div
                                         (for [item @items-atom]
                                           [:div.p-2.odd:bg-red-50.even:bg-green-50
                                            {:id (item-div-id item)}
                                            [:div (:title item)]
                                            [:div
                                             [:button.text-red-600.hover:underline
                                              {:hx-delete  (str "/htmx/delete-with-confirmation/items/" (:id item))
                                               :hx-target  (item-div-id-ref item)
                                               :hx-swap    "outerHTML"
                                               :hx-confirm "Are you sure you wish to delete this item?"}
                                              "delete"]

                                             ]
                                            [:div
                                             [:button.text-red-600.hover:underline
                                              {:hx-delete (str "/htmx/delete-with-confirmation/items/" (:id item))
                                               :hx-target (item-div-id-ref item)
                                               :hx-swap   "outerHTML"
                                               :_         "on htmx:confirm(issueRequest)
             halt the event
             call Swal.fire({title: 'Confirm', text:'Do you want to continue?', showDenyButton: true,\n  showCancelButton: true,\n  confirmButtonText: 'Save',})
             if result.isConfirmed issueRequest()"} "delete 2"]
                                             ]
                                            ]
                                           )
                                         ]
                                        (layout)
                                        (s/ok))]
                       (assoc ctx :response response)))})


(def delete-handler {:name ::delete
                     :enter
                     (fn [ctx]
                       (let [item-id (-> ctx :request :path-params :item-id)]
                         (swap! items-atom
                                (fn [items]
                                  (remove (fn [item]
                                            (= (str (:id item)) item-id)) items))))
                       (assoc ctx :response (s/ok nil)))})

(def routes
  #{["/htmx/delete-with-confirmation" :get [root-handler]]
    ["/htmx/delete-with-confirmation/items/:item-id" :delete [delete-handler]]})
