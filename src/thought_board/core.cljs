(ns thought-board.core
  (:require [reagent.core :as reagent :refer [atom]]
            [datascript.core :as d]
            [posh.reagent :as p]))


(enable-console-print!)

;; define your app data so that it doesn't get over-written on reload

(defonce schema {:card/column       {:db/valueType :db.type/ref}
                 :card/tags         {:db/valueType :db.type/ref
                                     :db/cadinality :db.cardinality/many}
                 :column/board      {:db/valueType :db.type/ref}})

(defonce data [{:db/id 10 :board/title "Demo Board" :board/slug "demo-board"}

               {:db/id 100 :column/title "Backlog"      :column/order 0 :column/board 10}
               {:db/id 101 :column/title "To Do"        :column/order 1 :column/board 10}
               {:db/id 102 :column/title "In Progress"  :column/order 2 :column/board 10}
               {:db/id 103 :column/title "Released"     :column/order 3 :column/board 10}

               {:db/id 200
                :card/title "Build MVP"
                :card/description "cards, columns, board"
                :card/order 0
                :card/column 101}])



(defonce conn
    (let [conn_ (d/create-conn schema)]
        (d/transact! conn_ data)
        (p/posh! conn_)
        conn_))

; queries


(defonce app-state (reagent/atom {:slug "demo-board"}))


(defn q-children [child-key order-key db id]
    (let [q  [:find '?child '?order
              :in '$ '?id
              :where ['?child child-key '?id]
                     ['?child order-key '?order]]]
     (->> @(p/q q db id)
        (map #(zipmap [:id :order] %))
        (sort-by :order)
        (map :id))))


(defn ulid [component props ids]
  [:ul (map #(vector :li {:key %} [component (assoc props :id %)]) ids)])


(defn card [{:keys [db id]}]
    (let [{title :card/title
           desc :card/description} @(p/pull db [:card/title :card/description] id)]
      [:div
        [:h3 title]
        [:p desc]]))


(defn column [{:keys [db id]}]
    (let [{title :column/title} @(p/pull db [:column/title] id)
          cards                 (q-children :card/column :card/order db id)]
      [:div
        [:h2 title]
        (ulid card {:db db} cards)]))


(defn board [{:keys [db id]}]
  (let [{title :board/title} @(p/pull db [:board/title] id)
        cols                 (q-children :column/board :column/order db id)]
    [:div
        [:h1 title]
        (ulid column {:db db} cols)]))


(defn board-not-found [{:keys [slug]}]
    [:div
        [:h1 "Not Found"]
        [:p "Sorry, we don't have any boards with the slug '"
            slug
            "'."]])

; TODO routing, DI

(def get-board '[:find [?id]
                 :in $ ?slug
                 :where [?id :board/slug ?slug]])

(defn app [{:keys [db state]}]
    (let [{slug :slug} @state
          [id]         @(p/q get-board db slug)]
      (cond (nil? id) [board-not-found {:slug slug}]
            :else     [board {:db db :id id}])))


(reagent/render-component [app {:db conn :state app-state}]
                          (. js/document (getElementById "app")))

(defn on-js-reload [])
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
