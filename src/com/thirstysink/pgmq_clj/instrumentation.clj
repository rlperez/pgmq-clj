(ns com.thirstysink.pgmq-clj.instrumentation
  (:require [clojure.spec.test.alpha :as stest]))

(def ^:dynamic instrumentation-enabled? (boolean (System/getenv "PGMQCLJ_INSTRUMENTAION_ENABLED")))

(defn- instrument-namespace
  [ns action]
  (doseq [[name _] (ns-publics ns)]
    (action (symbol (str ns) (str name)))))

(defn enable-instrumentation
  "Enables `clojure.specs.alpha` specs instrumentation.
  [Learn more](https://github.com/clojure/spec.alpha). If
  no namespace is provided it will instrument `com.thirstysink.pgmq-clj.core`."
  ([]
   (enable-instrumentation `com.thirstysink.pgmq-clj.core))
  ([ns]
   (instrument-namespace ns stest/instrument)))

(defn disable-instrumentation
  "Disables `clojure.specs.alpha` specs instrumentation.
  [Learn more](https://github.com/clojure/spec.alpha). If
  no namespace is provided it will disable instrumentation
  for `com.thirstysink.pgmq-clj.core`."
  ([]
   (disable-instrumentation `com.thirstysink.pgmq-clj.core))
  ([ns]
   (instrument-namespace ns stest/unstrument)))
