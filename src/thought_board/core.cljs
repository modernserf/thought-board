(ns thought-board.core
  (:require [reagent.core :as reagent :refer [atom]]
            [datascript.core :as d]))


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
        conn_))

; queries


(defonce app-state (reagent/atom {:slug "demo-board"}))

(defn ul [f items]
    [:ul (map (fn [value]
                [:li {:key (first value)} (f value)])
              items)])

(def get-card '[:find [?title ?description]
                :in $ ?id
                :where [?id :card/title ?title]
                       [?id :card/description ?description]])

(defn card [{:keys [db id]}]
    (let [[title description] (d/q get-card @db id)]
      [:div
        [:h3 title]
        [:p description]]))

(def get-col-title '[:find [?title]
                     :in $ ?id
                     :where [?id :column/title ?title]])

(def get-cards '[:find ?card ?order
                 :in $ ?id
                 :where [?card :card/column ?id]
                        [?card :card/order ?order]])

(defn column [{:keys [db id]}]
    (let [[title]   (d/q get-col-title @db id)
          cards     (d/q get-cards @db id)]
      [:div
        [:h2 title]
        (ul (fn [[id]] [card {:id id :db db}]) cards)]))

(defn board-not-found [{:keys [slug]}]
    [:div
        [:h1 "Not Found"]
        [:p "Sorry, we don't have any boards with the slug '"
            slug
            "'."]])

(def get-title '[:find [?title]
                 :in $ ?slug
                 :where [?id :board/slug ?slug]
                        [?id :board/title ?title]])

(def get-cols '[:find ?column ?order
                :in $ ?slug
                :where [?id :board/slug ?slug]
                       [?column :column/board ?id]
                       [?column :column/order ?order]])

(defn board [{:keys [db state]}]
  (let [slug    (:slug @state)
        [title] (d/q get-title @db slug)
        cols    (d/q get-cols @db slug)]
     (cond (nil? title) [board-not-found {:slug slug}]
           :else [:div
                    [:h1 title]
                    (ul (fn [[id]] [column {:id id :db db}]) cols)])))


; TODO routing, DI

(defn app [props]
    [board props])


(reagent/render-component [app {:db conn :state app-state}]
                          (. js/document (getElementById "app")))

(defn on-js-reload [])
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
