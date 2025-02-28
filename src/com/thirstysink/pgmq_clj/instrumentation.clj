(ns com.thirstysink.pgmq-clj.instrumentation
  (:require [clojure.spec.test.alpha :as stest]))

(def ^:dynamic instrumentation-enabled?
  "A flag that indicates if instrumentation is enabled.
  This is determined by the value of the environment variable `PGMQCLJ_INSTRUMENTAION_ENABLED`.
  If the environment variable is set, the value will be true; otherwise, false."
  (boolean (System/getenv "PGMQCLJ_INSTRUMENTAION_ENABLED")))

(defn- instrument-namespace
  [ns action]
  (doseq [[name _] (ns-publics ns)]
    (action (symbol (str ns) (str name)))))

(defn enable-instrumentation
  "Enables `clojure.specs.alpha` specs instrumentation. If
  no namespace `ns` is provided it will instrument
  `com.thirstysink.pgmq-clj.core`.

  [Learn more](https://github.com/clojure/spec.alpha)"
  ([]
   (enable-instrumentation `com.thirstysink.pgmq-clj.core))
  ([ns]
   (instrument-namespace ns stest/instrument)))

(defn disable-instrumentation
  "Disables `clojure.specs.alpha` specs instrumentation. If
  no namespace `ns` is provided it will disable instrumentation
  for `com.thirstysink.pgmq-clj.core`.

  [Learn more](https://github.com/clojure/spec.alpha)"

  ([]
   (disable-instrumentation `com.thirstysink.pgmq-clj.core))
  ([ns]
   (instrument-namespace ns stest/unstrument)))
