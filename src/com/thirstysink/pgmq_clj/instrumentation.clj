(ns com.thirstysink.pgmq-clj.instrumentation
  (:require [clojure.spec.test.alpha :as stest]
            [clojure.string :as cs]))

(def ^:dynamic instrumentation-enabled? (boolean (System/getenv "PGMQCLJ_INSTRUMENTAION_ENABLED")))

(defn- instrument-namespace
  [ns action]
  (doseq [[name _] (ns-publics ns)]
    (action (symbol (str ns) (str name)))))

(defn enable-instrumentation
  ([]
   (println (clojure.string/join "NAMESPACE " *ns*))
   (enable-instrumentation *ns*))
  ([ns]
   (instrument-namespace ns stest/instrument)))

(defn disable-instrumentation
  ([]
   (disable-instrumentation *ns*))
  ([ns]
   (instrument-namespace ns stest/unstrument)))
