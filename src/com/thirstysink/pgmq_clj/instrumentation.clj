(ns com.thirstysink.pgmq-clj.instrumentation
  (:require [clojure.spec.test.alpha :as stest]))

(def ^:dynamic instrumentation-enabled? (boolean (System/getenv "PGMQCLJ_INSTRUMENTAION_ENABLED")))

(defn- instrument-namespace
  [ns action]
  (doseq [[name _] (ns-publics ns)]
    (action (symbol (str ns) (str name)))))

(defn enable-instrumentation [ns]
  (instrument-namespace ns stest/instrument))

(defn disable-instrumentation [ns]
  (instrument-namespace ns stest/unstrument))
