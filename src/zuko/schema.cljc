(ns zuko.schema
  #?(:cljs (:refer-clojure :exclude [Var]))
  (:require #?(:clj  [schema.core :as s]
               :cljs [schema.core :as s :include-macros true])))

;; single element in a rule. These have extended to be very flexible.
(def EntityPropertyElt s/Any)

;; simple pattern containing a single element. e.g. [?v]
(def EntityPattern [(s/one s/Symbol "entity")])

;; two or three element pattern.
;; e.g. [?s :property]
;;      [:my/id ?property ?value]
(def EntityPropertyPattern
  [(s/one EntityPropertyElt "entity")
   (s/one EntityPropertyElt "property")
   (s/optional s/Any "value")])

;; The full pattern definition, with 1, 2 or 3 elements
(def EPVPattern
  (s/if #(= 1 (count %))
    EntityPattern
    EntityPropertyPattern))

;; Less restrictive than EPVPattern, because this is called at runtime
(s/defn epv-pattern? :- s/Bool
  [pattern :- [s/Any]]
  (and (vector? pattern)
       (let [f (first pattern)]
         (and (boolean f) (not (seq? f))))))

(def var-marker? #{\? \%})

(def Var (s/constrained s/Symbol #(or (var-marker? (first (name %)))
                                      (var-marker? (first (namespace %))))))

(s/defn vartest? :- s/Bool
  [x]
  (and (symbol? x) (boolean (or (var-marker? (first (name x)))
                                (var-marker? (first (namespace x)))))))

(defn ^:deprecated vars [c] (filter vartest? c))

(s/defn filter-pattern? :- s/Bool
  [pattern :- [s/Any]]
  (and (vector? pattern) (seq? (first pattern)) (nil? (second pattern))))

(defn eval-pattern?
  "eval bindings take the form of [expression var] where the
   expression is a list-based s-expression. It binds the var
   to the result of the expression."
  [p]
  (and (vector? p) (= 2 (count p)) 
       (let [[e v] p]
         (and (vartest? v) (sequential? e) (not (vector? e))))))

(def operators ['and 'AND 'or 'OR 'not 'NOT 'optional 'OPTIONAL])

(s/defn op-pattern? :- s/Bool
  [[op :as pattern] :- [s/Any]]
  (and (seq? pattern) (boolean (some (partial = op) operators))))

(def Operators (apply s/enum operators))

(defn unnested-list?
  [[fl :as l]]
  (and (vector? l) (seq? fl) (not-any? seq? fl)))

;; filters are a vector with an executable list destined for eval
(def FilterPattern (s/constrained [(s/one [s/Any] "Predicate")]
                                  unnested-list?))

(def EvalPattern (s/constrained [(s/one [s/Any] "Expression")
                                 (s/one Var "Binding var")]
                                unnested-list?))

(declare Pattern)

(def OpPattern (s/constrained [(s/one Operators "operator")
                               (s/one (s/recursive #'Pattern) "first pattern")
                               (s/recursive #'Pattern)]
                              seq?)) 

(def Pattern (s/if seq?
               OpPattern
               (s/if (comp seq? first)
                 (s/if (comp nil? second) FilterPattern EvalPattern)
                 EPVPattern)))

(def Value s/Any)

(def Results [[Value]])

(def EntityPropAxiomElt
  (s/cond-pre s/Keyword s/Str #?(:clj Long :cljs s/Num)))

(def EntityPropValAxiomElt
  (s/conditional (complement symbol?) s/Any))

(def Triple
  [(s/one s/Any "entity")
   (s/one s/Any "property")
   (s/one s/Any "value")])

(def Axiom
  [(s/one EntityPropAxiomElt "entity")
   (s/one EntityPropAxiomElt "property")
   (s/one EntityPropValAxiomElt "value")])

