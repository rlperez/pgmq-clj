(ns com.thirstysink.pgmq-clj.core-test
  (:require [clojure.test :refer [deftest is use-fixtures testing]]
            [clojure.spec.alpha :as s]
            [com.thirstysink.pgmq-clj.core :as core]
            [com.thirstysink.pgmq-clj.db.adapter :as adapter]
            [com.thirstysink.pgmq-clj.instrumentation :as inst]
            [com.thirstysink.util.db :as db]
            [clojure.core :as c]))

(defonce container (db/pgmq-container))

(use-fixtures :once
  (fn [tests]
    (try
      (inst/enable-instrumentation)
      (db/start-postgres-container container)
      (tests)
      (finally
        (inst/disable-instrumentation)
        (db/stop-postgres-container container)))))

(deftest create-and-drop-queue-test
  (let [adapter (db/setup-adapter container)
        queue-name "test_queue"]

    (core/create-queue adapter queue-name)

    (let [result (adapter/query adapter "SELECT * FROM pgmq.list_queues() WHERE queue_name = ?;" [queue-name])]
      (is (= 1 (count result)))
      (is (= queue-name (:queue_name (first result)))))))

(deftest create-queue-name-test
  (let [adapter (db/setup-adapter container)
        expected-msg #"Call to com.thirstysink.pgmq-clj.core/create-queue did not conform to spec."]
    (testing "create-queue throws exception with an nil adapter"
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           expected-msg
           (core/create-queue nil "test-queue"))))
    (testing "create-queue throws exception with an invalid adapter"
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           expected-msg
           (core/create-queue [] "test-queue"))))
    (testing "create-queue throws exception with an empty queue-name"
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           expected-msg
           (core/create-queue adapter ""))))
    (testing "create-queue throws exception with a nil queue-name"
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           expected-msg
           (core/create-queue adapter nil))))
    (testing "create-queue throws exception with a non string queue-name"
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           expected-msg
           (core/create-queue adapter 1))))))

(deftest drop-queue-name-test
  (let [adapter (db/setup-adapter container)
        expected-msg #"Call to com.thirstysink.pgmq-clj.core/drop-queue did not conform to spec."]
    (testing "drop-queue throws exception with an nil adapter"
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           expected-msg
           (core/drop-queue nil "test-queue"))))
    (testing "drop-queue throws exception with an invalid adapter"
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           expected-msg
           (core/drop-queue [] "test-queue"))))
    (testing "drop-queue throws exception with an empty queue-name"
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           expected-msg
           (core/drop-queue adapter ""))))
    (testing "drop-queue throws exception with a nil queue-name"
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           expected-msg
           (core/drop-queue adapter nil))))
    (testing "drop-queue throws exception with a non string queue-name"
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           expected-msg
           (core/drop-queue adapter 1))))))

(deftest read-message-visibility-time-test
  (let [adapter (db/setup-adapter container)
        queue-name "test-queue"]
    (testing "send-message function"
      (core/create-queue adapter queue-name)
      (let [payload {:foo "bar"}
            result (core/send-message adapter queue-name payload)]
        (is (some? result))
        (is (number? result)))
      (core/drop-queue adapter queue-name))

    (testing "read-message should respect visibility time"
      (core/create-queue adapter queue-name)
      (let [visibility-time 1
            quantity 2]
        ;; Send two messages to a fresh queue
        (let [payload {:foo "bar"}]
          (core/send-message adapter queue-name payload))
        (let [payload {:foo "baz"}]
          (core/send-message adapter queue-name payload))
        ;; Filtering on a message with a foo set to bar we should only get one.
        (let [result-filter (core/read-message adapter queue-name visibility-time quantity {:foo "bar"})]
          (is (seq result-filter))
          (is (= 1 (count result-filter)))
          (is (= (get-in (first result-filter) [:msg_id]) 1)))
        ;; Reading for foo bar again should be empty due to visibility rules
        (let [result-bar-before (core/read-message adapter queue-name visibility-time quantity {:foo "bar"})]
          (is (empty? result-bar-before)))
        ;; Reading unfiltered now should also fetch foo baz
        (let [result-baz-before (core/read-message adapter queue-name visibility-time quantity {})]
          (is (seq result-baz-before))
          (is (= 1 (count result-baz-before)))
          (is (= (get-in (first result-baz-before) [:msg_id]) 2)))
        (Thread/sleep 1500)
        ;; After sleeping past the visibility time we should have both foos, bar and baz
        (let [result-after (core/read-message adapter queue-name visibility-time quantity {})]
          (is (= 2 (count result-after)))))
      (core/drop-queue adapter queue-name))))

(deftest read-message-spec-test
  (let [adapter (db/setup-adapter container)]
    (testing "read-message spec validation"
      (let [valid-args {:adapter adapter
                        :queue-name "test-queue"
                        :visibility_time 30
                        :quantity 2}
            invalid-args {:adapter adapter
                          :queue-name ""
                          :visibility_time -1
                          :quantity 0}]
        (is (s/valid? ::core/adapter (:adapter valid-args)) "Adapter should satisfy the ::adapter spec")
        (is (s/valid? ::core/queue-name (:queue-name valid-args)) "Queue name should satisfy the ::queue-name spec")
        (is (s/valid? ::core/visibility_time (:visibility_time valid-args)) "Visibility time should satisfy the ::visibility_time spec")
        (is (s/valid? ::core/quantity (:quantity valid-args)) "Quantity should satisfy the ::quantity spec")

        (is (not (s/valid? ::core/queue-name (:queue-name invalid-args))) "Invalid queue name should fail the ::queue-name spec")
        (is (not (s/valid? ::core/visibility_time (:visibility_time invalid-args))) "Negative visibility_time should fail the ::visibility_time spec")
        (is (not (s/valid? ::core/quantity (:quantity invalid-args))) "Zero quantity should fail the ::quantity spec")))))

(deftest send-message-spec-test
  (let [adapter (db/setup-adapter container)]
    (testing "send-message spec validation"
      (let [valid-args {:adapter adapter
                        :queue-name "test-queue"
                        :payload {:foo "bar"}}
            invalid-args {:adapter adapter
                          :queue-name ""
                          :payload nil}]
        (is (s/valid? ::core/adapter (:adapter valid-args)) "Adapter should satisfy the :core/adapter spec")
        (is (s/valid? ::core/queue-name (:queue-name valid-args)) "Queue name should satisfy the ::queue-name spec")
        (is (s/valid? ::core/json (:payload valid-args)) "Payload should be a valid string")

        (is (not (s/valid? ::core/queue-name (:queue-name invalid-args))) "Invalid queue name should fail the ::queue-name spec")
        (is (not (s/valid? ::core/json (:payload invalid-args))) "Nil payload should fail the string? spec")))))
