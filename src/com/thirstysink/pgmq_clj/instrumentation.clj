(ns com.thirstysink.pgmq-clj.instrumentation
  (:require [clojure.spec.test.alpha :as stest]))

(def ^:dynamic instrumentation-enabled? (boolean (System/getenv "PGMQCLJ_INSTRUMENTAION_ENABLED")))

(defn- instrument-namespace
  [ns action]
  (doseq [[name _] (ns-publics ns)]
    (action (symbol (str ns) (str name)))))

(defn enable-instrumentation
  ([]
   (enable-instrumentation `com.thirstysink.pgmq-clj.core))  ;; Use *ns* to instrument the current namespace
  ([ns]
   (instrument-namespace ns stest/instrument)))  ;; Ensure the correct namespace is instrumented

(defn disable-instrumentation
  ([]
   (disable-instrumentation `com.thirstysink.pgmq-clj.core))  ;; Use *ns* to disable the current namespace's instrumentation
  ([ns]
   (instrument-namespace ns stest/unstrument)))  ;; Uninstrument the namespace
