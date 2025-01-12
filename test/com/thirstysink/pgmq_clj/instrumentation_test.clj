(ns com.thirstysink.pgmq-clj.instrumentation-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [deftest is testing]]
            [com.thirstysink.pgmq-clj.core :as core]
            [com.thirstysink.pgmq-clj.instrumentation :as inst])
  (:import [java.time Instant]))

(s/def ::test-arg string?)

(s/fdef test-fn
  :args (s/cat :_arg ::test-arg))

(defn test-fn [_arg] nil)

(deftest test-timestamp-spec
  (testing "Valid string formats"
    (is (s/valid? ::core/timestamp "2025-01-11T14:30:00"))
    (is (s/valid? ::core/timestamp "2025-01-11T14:30:00.123456"))
    (is (s/valid? ::core/timestamp "2025-01-11T14:30:00.000000"))
    (is (s/valid? ::core/timestamp "2025-01-11T14:30:00+00:00"))
    (is (s/valid? ::core/timestamp (Instant/now))))
  (testing "Invalid string formats"
    (is (not (s/valid? ::core/timestamp "11/01/2025 14:30:00")))
    (is (not (s/valid? ::core/timestamp "2025-01-11 14:60:00")))
    (is (not (s/valid? ::core/timestamp "2025-01-11T14:60:00")))
    (is (not (s/valid? ::core/timestamp "2025-01-11T14:30:00+25:00")))
    (is (not (s/valid? ::core/timestamp "2025-01-11T14:30:00:00")))
    (is (not (s/valid? ::core/timestamp "not an instant"))))
  (testing "Instant object"
    (is (s/valid? ::core/timestamp (java.time.Instant/now))))
  (testing "Invalid Instant object"
    (is (not (s/valid? ::core/timestamp "not an instant")))))

(deftest enable-and-disable-instrumentation-test
  (inst/enable-instrumentation `com.thirstysink.pgmq-clj.instrumentation-test)

  (is (thrown-with-msg?
       clojure.lang.ExceptionInfo
       #"Call to com.thirstysink.pgmq-clj.instrumentation-test/test-fn did not conform to spec"
       (test-fn 99)))

  (inst/disable-instrumentation `com.thirstysink.pgmq-clj.instrumentation-test)

  (is (try
        (test-fn 99)
        true
        (catch Exception e
          (do
            (println "Exception was thrown:" e)
            false)))))
