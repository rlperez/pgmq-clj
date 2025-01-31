(ns com.thirstysink.pgmq-clj.instrumentation-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [deftest is]]
            [com.thirstysink.pgmq-clj.instrumentation :as inst]))

(s/def ::test-arg string?)

(s/fdef test-fn
  :args (s/cat :_arg ::test-arg))

(defn test-fn [_arg] nil)

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
