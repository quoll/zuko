(ns zuko.test-entity
  #?(:clj (:refer-clojure :exclude [read-string]))
  (:require [zuko.entity.writer :refer [entities->triples entity-update->triples ident-map->triples]]
            [zuko.entity.reader :refer [graph->entities ref->entity]]
            [zuko.helper-stub :as test-helper]
            [asami.graph :refer [graph-transact]]
            [asami.multi-graph]
            [asami.memory :refer [empty-graph]]
            [asami.core :refer [q]]
            [qtest.core :refer [with-fresh-gen]]
            #?(:clj  [clojure.edn :refer [read-string]]
               :cljs [cljs.reader :refer [read-string]])
            #?(:clj  [schema.test :as st :refer [deftest]]
               :cljs [schema.test :as st :refer-macros [deftest]])
            #?(:clj  [clojure.test :as t :refer [is]]
               :cljs [clojure.test :as t :refer-macros [is]]))
  #?(:clj (:import [java.time ZonedDateTime])
     :cljs (:import [goog.date DateTime])))

(defn parseDateTime [s]
  #?(:clj (ZonedDateTime/parse s)
     :cljs (DateTime.fromRfc822String s)))

(t/use-fixtures :once st/validate-schemas)

(defn assert-data
  [graph data]
  (graph-transact graph 0 data nil))

(defn retract-data
  [graph data]
  (graph-transact graph 0 nil data))

(defn string->graph-set [s]
  (let [d (read-string s)]
    (set (entities->triples (test-helper/new-graph) d))))

(deftest test-encode-from-string
  (let [m1 (string->graph-set "[{:prop \"val\"}]")
        m2 (string->graph-set "[{:prop \"val\", :p2 2}]")
        m3 (string->graph-set (str "[{:prop \"val\","
                                   "  :p2 22,"
                                   "  :p3 [42, 54]}]"))
        m4 (string->graph-set (str "[{:prop \"val\"},"
                                   " {:prop \"val2\"}]"))
        m5 (string->graph-set (str "[{:prop \"val\","
                                   "  :arr ["
                                   "    {:a 1},"
                                   "    {:a 2},"
                                   "    [\"nested\"]"
                                   "]}]"))]
    (is (= #{[:test/n1 :db/ident :test/n1]
             [:test/n1 :tg/entity true]
             [:test/n1 :prop "val"]} m1))
    (is (= #{[:test/n1 :db/ident :test/n1]
             [:test/n1 :tg/entity true]
             [:test/n1 :prop "val"]
             [:test/n1 :p2 2]} m2))
    (is (= #{[:test/n1 :db/ident :test/n1]
             [:test/n1 :tg/entity true]
             [:test/n1 :prop "val"]
             [:test/n1 :p2 22]
             [:test/n1 :p3 :test/n2]
             [:test/n1 :tg/sub :test/n2]
             [:test/n2 :tg/first 42]
             [:test/n2 :tg/rest :test/n3]
             [:test/n3 :tg/first 54]
             [:test/n2 :tg/contains 42]
             [:test/n2 :tg/contains 54]} m3))
    (is (= #{[:test/n1 :db/ident :test/n1]
             [:test/n1 :tg/entity true]
             [:test/n1 :prop "val"]
             [:test/n2 :db/ident :test/n2]
             [:test/n2 :tg/entity true]
             [:test/n2 :prop "val2"]} m4))
    (is (= #{[:test/n1 :db/ident :test/n1]
             [:test/n1 :tg/entity true]
             [:test/n1 :prop "val"]
             [:test/n1 :arr :test/n2]
             [:test/n1 :tg/sub :test/n2]
             [:test/n2 :tg/first :test/n3]
             [:test/n2 :tg/rest :test/n4]
             [:test/n3 :a 1]
             [:test/n1 :tg/sub :test/n3]
             [:test/n4 :tg/first :test/n5]
             [:test/n4 :tg/rest :test/n6]
             [:test/n5 :a 2]
             [:test/n1 :tg/sub :test/n5]
             [:test/n6 :tg/first :test/n7]
             [:test/n1 :tg/sub :test/n7]
             [:test/n7 :tg/first "nested"]
             [:test/n7 :tg/contains "nested"]
             [:test/n2 :tg/contains :test/n3]
             [:test/n2 :tg/contains :test/n5]
             [:test/n2 :tg/contains :test/n7]} m5))))

(defn entities->graph [coll]
  (set (entities->triples (test-helper/new-graph) coll)))

(deftest test-encode
  (let [m1 (entities->graph [{:prop "val"}])
        m2 (entities->graph [{:prop "val", :p2 2}])
        m3 (entities->graph [{:prop "val", :p2 22, :p3 [42 54]}])
        m4 (entities->graph [{:prop "val"} {:prop "val2"}])
        m5 (entities->graph [{:prop "val"
                              :arr [{:a 1} {:a 2} ["nested"]]}])
        m6 (entities->graph [{:prop "val", :p2 22, :p3 []}])]
    (is (= #{[:test/n1 :db/ident :test/n1]
             [:test/n1 :tg/entity true]
             [:test/n1 :prop "val"]} m1))
    (is (= #{[:test/n1 :db/ident :test/n1]
             [:test/n1 :tg/entity true]
             [:test/n1 :prop "val"]
             [:test/n1 :p2 2]} m2))
    (is (= #{[:test/n1 :db/ident :test/n1]
             [:test/n1 :tg/entity true]
             [:test/n1 :prop "val"]
             [:test/n1 :p2 22]
             [:test/n1 :p3 :test/n2]
             [:test/n1 :tg/sub :test/n2]
             [:test/n2 :tg/first 42]
             [:test/n2 :tg/rest :test/n3]
             [:test/n3 :tg/first 54]
             [:test/n2 :tg/contains 42]
             [:test/n2 :tg/contains 54]} m3))
    (is (= #{[:test/n1 :db/ident :test/n1]
             [:test/n1 :tg/entity true]
             [:test/n1 :prop "val"]
             [:test/n2 :db/ident :test/n2]
             [:test/n2 :tg/entity true]
             [:test/n2 :prop "val2"]} m4))
    (is (= #{[:test/n1 :db/ident :test/n1]
             [:test/n1 :tg/entity true]
             [:test/n1 :prop "val"]
             [:test/n1 :arr :test/n2]
             [:test/n1 :tg/sub :test/n2]
             [:test/n2 :tg/first :test/n3]
             [:test/n1 :tg/sub :test/n3]
             [:test/n2 :tg/rest :test/n4]
             [:test/n3 :a 1]
             [:test/n4 :tg/first :test/n5]
             [:test/n1 :tg/sub :test/n5]
             [:test/n4 :tg/rest :test/n6]
             [:test/n5 :a 2]
             [:test/n6 :tg/first :test/n7]
             [:test/n1 :tg/sub :test/n7]
             [:test/n7 :tg/first "nested"]
             [:test/n7 :tg/contains "nested"]
             [:test/n2 :tg/contains :test/n3]
             [:test/n2 :tg/contains :test/n5]
             [:test/n2 :tg/contains :test/n7]} m5))
    (is (= #{[:test/n1 :db/ident :test/n1]
             [:test/n1 :tg/entity true]
             [:test/n1 :prop "val"]
             [:test/n1 :p2 22]
             [:test/n1 :p3 :tg/empty-list]} m6))))

(defn round-trip
  [data]
  (let [m (entities->triples empty-graph (seq data))
        new-db (assert-data empty-graph m)]
    (set (graph->entities new-db))))

(deftest test-round-trip
  (let [d1 #{{:prop "val"}}
        dr1 (round-trip d1)

        d2 #{{:prop "val", :p2 2}}
        dr2 (round-trip d2)

        d3 #{{:prop "val", :p2 22, :p3 [42 54]}}
        dr3 (round-trip d3)

        d4 #{{:prop "val"} {:prop "val2"}}
        dr4 (round-trip d4)

        d5 #{{:prop "val" :arr [{:a 1} {:a 2} ["nested"]]}}
        dr5 (round-trip d5)

        d6 #{{:prop "val" :arr [{:a 1} {:a 2} ["nested"]] :nested {}}}
        dr6 (round-trip d6)

        d7 #{{:prop "val" :arr (map identity [{:a 1} {:a 2} ["nested"]]) :nested {}}}
        dr7 (round-trip d7)

        d8 #{{:prop "val" :arr #{{:a 1} {:a 2} ["nested"]}}}
        dr8 (round-trip d8)

        d9 #{{:prop "val", :p2 22, :p3 []}}
        dr9 (round-trip d9)]

    (is (= d1 dr1))
    (is (= d2 dr2))
    (is (= d3 dr3))
    (is (= d4 dr4))
    (is (= d5 dr5))
    (is (= d6 dr6))
    (is (= d7 dr7))
    (is (= d8 dr8))
    (is (= d9 dr9))))

(defn generate-diff
  [o1 o2]
  (let [triples (entities->triples empty-graph [o1])
        props (filter (fn [[k v]] (or (number? v) (string? v))) o1)
        gr (assert-data empty-graph triples)
        id (ffirst (q (concat '[:find ?id :where] (map (fn [[k v]] ['?id k v]) props)) gr))
        [additions retractions] (entity-update->triples gr id o2)]
    [id additions retractions]))

(deftest test-updates
  (let [
        [id1 add1 ret1] (generate-diff {:a 1} {:a 2})
        [id2 add2 ret2] (generate-diff {:a 1 :b "foo"} {:a 2 :b "foo"})
        [id3 add3 ret3] (generate-diff {:a 1 :b "foo"} {:a 1 :b "bar"})
        [id4 add4 ret4] (generate-diff {:a 1 :b "foo" :c [10 11 12] :d {:x "a" :y "b"} :e [{:m 1} {:m 2}]}
                                       {:b "bar", :c [10 10 12], :e [{:m 1} {:m 2}], :bx "xxx"})]
    (is (= add1 [[id1 :a 2]]))
    (is (= ret1 [[id1 :a 1]]))
    (is (= add2 [[id2 :a 2]]))
    (is (= ret2 [[id2 :a 1]]))
    (is (= add3 [[id3 :b "bar"]]))
    (is (= ret3 [[id3 :b "foo"]]))
    (let [adds (filter #(#{:b :c :bx} (second %)) add4)
          dels (filter #(#{:a :b :c :d} (second %)) ret4)
          [[sid1 _ a1] [sid2 _ a2] :as ds] (filter (fn [[_ p _]] (#{:x :y} p)) ret4)]
      (is (= 3 (count adds)))
      (is (= 4 (count dels)))
      (is (= sid1 sid2))
      (is (= #{"a" "b"} #{a1 a2})))))

(defn get-node-ref
  [graph id]
  (ffirst (q [:find '?n :where ['?n :id id]] graph)))


(deftest test-ref->entity
  (let [data {:id "1234" :prop "value" :attribute 2}
        m (entities->triples empty-graph [data])
        graph' (assert-data empty-graph m)
        ref (get-node-ref graph' "1234")
        graph (assert-data graph' [[ref "Connected_To" ref]])
        obj1 (ref->entity graph ref)
        obj2 (ref->entity graph ref false #{"Connected_To"})]
    (is (= (assoc data "Connected_To" {:db/ident ref})
           obj1))
    (is (= data obj2))))

(deftest test-nested-ref->entity
  (let [d0 {:db/ident "abcd" :prop "nested" :attribute 5}
        data {:id "1234" :prop "value" :attribute 2 :sub {:db/ident "abcd"}}
        m (entities->triples empty-graph [d0 data])
        graph (assert-data empty-graph m)
        ref (get-node-ref graph "1234")
        obj1 (ref->entity graph ref)
        obj2 (ref->entity graph ref true)]
    (is (= data obj1))
    (is (= (assoc data :sub (dissoc d0 :db/ident)) obj2))))

(defn ident-map->graph
  ([m] (ident-map->graph m {}))
  ([m mp]
   (let [[triples result-map] (ident-map->triples empty-graph m mp)]
     [(set triples) result-map])))

(deftest test-ident-map->triples
  (with-fresh-gen
    (let [data {:id "1234" :prop "value" :attribute 2}
          [triples1 map1] (ident-map->graph data)
          node1 (ffirst triples1)
          data2 {:db/id node1 :prop "value" :attribute 2}
          [triples2 map2] (ident-map->graph data2)
          data3 {:db/id -1 :prop "value" :attribute 2}
          [triples3 map3] (ident-map->graph data3)
          node2 (get map3 -1)
          data4 {:db/id -1 :prop "value" :attribute 2}
          [triples4 map4] (ident-map->graph data4 {-1 :tg/node-101})
          data5 {:db/id -1 :prop "value" :attribute 2 :sub {:db/id -1}}
          [triples5 map5] (ident-map->graph data5)
          node3 (get map5 -1)
          data6 {:db/id -1 :prop "value" :attribute 2 :sub {:db/id -1}}
          [triples6 map6] (ident-map->graph data6 {-1 :tg/node-101})
          data7 {:db/id -1 :prop "value" :sub {:db/id -2} :elts [{:name "one"} {:db/id -2 :name "two"}]}
          [triples7 map7] (ident-map->graph data7)
          node4 (get map7 -1)
          node5 (get map7 -2)
          node6 (->> triples7 (filter #(= :elts (second %))) first last)
          node7 (->> triples7 (filter #(and (= :name (second %)) (= "one" (nth % 2)))) ffirst)
          node8 (->> triples7 (filter #(= :tg/rest (second %))) first last)
          data8 {:db/id -1 :prop #{"value1" "value2"} :attribute 2}
          [triples8 map8] (ident-map->graph data8)
          node9 (get map8 -1)]
      (is (empty? map1))
      (is (= #{[node1 :db/ident node1]
               [node1 :tg/entity true]
               [node1 :id "1234"]
               [node1 :prop "value"]
               [node1 :attribute 2]}
             triples1))
      (is (empty? map2))
      (is (= #{[node1 :db/ident node1]
               [node1 :tg/entity true]
               [node1 :prop "value"]
               [node1 :attribute 2]}
             triples2))
      (is (= #{[node2 :db/ident node2]
               [node2 :tg/entity true]
               [node2 :prop "value"]
               [node2 :attribute 2]}
             triples3))
      (is (= {-1 :tg/node-101} map4))
      (is (= #{[:tg/node-101 :db/ident :tg/node-101]
               [:tg/node-101 :tg/entity true]
               [:tg/node-101 :prop "value"]
               [:tg/node-101 :attribute 2]}
             triples4))
      (is (= {-1 node3} map5))
      (is (= #{[node3 :db/ident node3]
               [node3 :tg/entity true]
               [node3 :prop "value"]
               [node3 :attribute 2]
               [node3 :sub node3]}
             triples5))
      (is (= {-1 :tg/node-101} map6))
      (is (= #{[:tg/node-101 :db/ident :tg/node-101]
               [:tg/node-101 :tg/entity true]
               [:tg/node-101 :prop "value"]
               [:tg/node-101 :attribute 2]
               [:tg/node-101 :sub :tg/node-101]}
             triples6))
      (is (= {-1 node4 -2 node5} map7))
      (is (= #{[node4 :db/ident node4]
               [node4 :tg/entity true]
               [node4 :prop "value"]
               [node4 :sub node5]
               [node4 :tg/sub node5]
               [node4 :elts node6]
               [node4 :tg/sub node6]
               [node6 :tg/first node7]
               [node6 :tg/rest node8]
               [node4 :tg/sub node7]
               [node7 :name "one"]
               [node8 :tg/first node5]
               [node5 :name "two"]
               [node6 :tg/contains node7]
               [node6 :tg/contains node5]}
             triples7))
      (is (= #{[node9 :db/ident node9]
               [node9 :tg/entity true]
               [node9 :prop "value1"]
               [node9 :prop "value2"]
               [node9 :attribute 2]}
             triples8))
      )))

#?(:clj
(deftest test-multi-update
  (let [graph
        #asami.multi_graph.MultiGraph{:spo #:tg{:node-27367
                                                 {:db/ident #:tg{:node-27367 {:count 1 :tx 0}},
                                                  :tg/entity {true {:count 1 :tx 0}},
                                                  :value {"01468b1d3e089985a4ed255b6594d24863cfd94a647329c631e4f4e52759f8a9" {:count 1 :tx 0}},
                                                  :type {"sha256" {:count 1 :tx 0}},
                                                  :id {"4f390192" {:count 1 :tx 0}}}},
                                      :pos {:db/ident
                                            #:tg{:node-27367 #:tg{:node-27367 {:count 1 :tx 0}}},
                                            :tg/entity {true #:tg{:node-27367 {:count 1 :tx 0}}},
                                            :value {"01468b1d3e089985a4ed255b6594d24863cfd94a647329c631e4f4e52759f8a9" #:tg{:node-27367 {:count 1 :tx 0}}},
                                            :type {"sha256" #:tg{:node-27367 {:count 1 :tx 0}}},
                                            :id {"4f390192" #:tg{:node-27367 {:count 1 :tx 0}}}},
                                      :osp {:tg/node-27367 #:tg{:node-27367 #:db{:ident {:count 1 :tx 0}}},
                                            true #:tg{:node-27367 #:tg{:entity {:count 1 :tx 0}}},
                                            "01468b1d3e089985a4ed255b6594d24863cfd94a647329c631e4f4e52759f8a9"
                                            #:tg{:node-27367 {:value {:count 1 :tx 0}}},
                                            "sha256" #:tg{:node-27367 {:type {:count 1 :tx 0}}},
                                            "4f390192" #:tg{:node-27367 {:id {:count 1 :tx 0}}}}}
        id "verdict:AMP File Reputation:4f390192"
        m {:type "verdict",
           :disposition 2,
           :observable {:value "01468b1d3e089985a4ed255b6594d24863cfd94a647329c631e4f4e52759f8a9",
                        :type "sha256"},
           :disposition_name "Malicious",
           :valid_time {:start_time (parseDateTime "2017-12-05T12:45:32.192Z"),
                        :end_time (parseDateTime "2525-01-01T00:00Z")},
           :module-name "AMP File Reputation",
           :id "verdict:AMP File Reputation:4f390192"}
        new-graph (if-let [n (get-node-ref graph id)]
                    (let [[assertions retractions] (entity-update->triples graph n m)
                          assert-keys (set (map (fn [[a b c]] [a b]) assertions))
                          retract-existing (filter (fn [[a b c]] (assert-keys [a b])) retractions)]
                      (-> graph
                          (retract-data retract-existing)
                          (assert-data assertions)))
                    (let [[assertions] (ident-map->triples graph (assoc m :id id))]
                      (assert-data graph assertions)))]
    (is (= 4 (count (:spo new-graph)))))))

#?(:cljs (t/run-tests))
