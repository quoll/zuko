(ns ^{:doc "Reads structured data from a graph."
      :author "Paula Gearon"}
    zuko.entity.reader
  (:require [zuko.entity.general :as general :refer [tg-ns KeyValue EntityMap GraphType]]
            [zuko.node :as node]
            [schema.core :as s :refer [=>]]
            [clojure.string :as string]
            #?(:clj [cheshire.core :as j])))


(def MapOrList (s/cond-pre EntityMap [s/Any]))

(defn get-tg-first
  "Finds the tg/first property in a map, and gets the value."
  [struct]
  (let [first-pair? (fn [[k v :as p]]
                     (and (= tg-ns (namespace k))
                          (string/starts-with? (name k) "first")
                          p))]
    (some first-pair? struct)))

(s/defn property-values :- [KeyValue]
  "Return all the property/value pairs for a given entity in the store.
   Skips non-keyword properties, as these are not created by tg.entity"
  [graph :- GraphType
   entity :- s/Any]
  (->> (node/find-triple graph [entity '?p '?o])
       (filter (comp keyword? first))))


(s/defn check-structure :- (s/maybe [KeyValue])
  "Determines if a value represents a structure. If so, return the property/values for it.
   Otherwise, return nil."
  [graph :- GraphType
   prop :- s/Any
   v :- s/Any]
  (if (and (not (#{:db/ident :db/id} prop)) (node/node-type? graph prop v))
    (let [data (property-values graph v)]
      data)))


(declare pairs->struct recurse-node)

(s/defn build-list :- [s/Any]
  "Takes property/value pairs and if they represent a list node, returns the list.
   else, nil."
  [graph :- GraphType
   seen :- #{s/Any}
   pairs :- [KeyValue]]
  ;; convert the data to a map
  (let [st (into {} pairs)]
    ;; if the properties indicate a list, then process it
    (when-let [first-prop-elt (get-tg-first st)]
      (let [remaining (:tg/rest st)
            [_ first-elt] (recurse-node graph seen first-prop-elt)]
        (assert first-elt)
        ;; recursively build the list
        (if remaining
          (cons first-elt (build-list graph seen (property-values graph remaining)))
          (list first-elt))))))


(s/defn recurse-node :- s/Any
  "Determines if the val of a map entry is a node to be recursed on, and loads if necessary.
  If referring directly to a top level node, then short circuit and return the ID"
  [graph :- GraphType
   seen :- #{s/Keyword}
   [prop v :as prop-val] :- KeyValue]
  (if-let [pairs (check-structure graph prop v)]
    (if (some #(= :tg/entity (first %)) pairs)
      [prop (some (fn [[k v]] (if (= :id k) v)) pairs)]
      [prop (or (build-list graph seen pairs)
                (pairs->struct graph pairs (conj seen v)))])
    prop-val))


(s/defn pairs->struct :- EntityMap
  "Uses a set of property-value pairs to load up a nested data structure from the graph"
  ([graph :- GraphType
    prop-vals :- [KeyValue]] (pairs->struct graph prop-vals #{}))
  ([graph :- GraphType
    prop-vals :- [KeyValue]
    seen :- #{s/Keyword}]
   (if (some (fn [[k _]] (= :tg/first k)) prop-vals)
     (build-list graph seen prop-vals)
     (->> prop-vals
          (remove (comp #{:db/id :db/ident :tg/entity} first))
          (remove (comp seen second))
          (map (partial recurse-node graph seen))
          (into {})))))


(s/defn ref->entity :- EntityMap
  "Uses an id node to load up a nested data structure from the graph.
   Accepts a value that identifies the internal node."
  ([graph :- GraphType
    entity-id :- s/Any]
   (ref->entity graph entity-id nil))
  ([graph :- GraphType
    entity-id :- s/Any
    exclusions :- (s/maybe #{(s/cond-pre s/Keyword s/Str)})]
   (let [prop-vals (property-values graph entity-id)
         pvs (if (seq exclusions)
               (remove (comp exclusions first) prop-vals)
               prop-vals)]
     (pairs->struct graph pvs))))


(s/defn ident->entity :- EntityMap
  "Converts data in a database to a data structure suitable for JSON encoding
   Accepts an internal node identifier to identify the entity object"
  [graph :- GraphType
   ident :- s/Any]
  ;; find the entity by its ident. Some systems will make the id the entity id,
  ;; and the ident will be separate, so look for both.
  (let [eid (or (ffirst (node/find-triple graph '[?eid :db/id ident]))
                (ffirst (node/find-triple graph '[?eid :db/ident ident])))]
    (ref->entity graph eid)))

(s/defn graph->entities :- [EntityMap]
  "Pulls all top level entities out of a store"
  ([graph :- GraphType]
   (graph->entities graph nil))
  ([graph :- GraphType
    exclusions :- (s/maybe #{s/Keyword})]
   (->> (node/find-triple graph '[?e :tg/entity true])
        (map first)
        (map #(ref->entity graph % exclusions)))))

#?(:clj
   (defn json-generate-string
     ([data] (j/generate-string data))
     ([data indent]
      (j/generate-string
       data
       (assoc j/default-pretty-print-options
              :indentation (apply str (repeat indent \space))))))

   :cljs
   (defn json-generate-string
     ([data] (.stringify js/JSON (clj->js data)))
     ([data indent] (.stringify js/JSON (clj->js data) nil indent))))


(s/defn graph->str :- s/Str
  "Reads a store into JSON strings"
  ([graph :- GraphType]
   (json-generate-string (graph->entities graph)))
  ([graph :- GraphType, indent :- s/Num]
   (json-generate-string (graph->entities graph) indent)))
