(ns com.thirstysink.pgmq-clj.core-test
  (:require [clojure.test :refer [deftest is use-fixtures testing]]
            [clojure.spec.alpha :as s]
            [com.thirstysink.pgmq-clj.core :as core]
            [com.thirstysink.pgmq-clj.db.adapter :as adapter]
            [com.thirstysink.pgmq-clj.instrumentation :as inst]
            [com.thirstysink.util.db :as db]
            [clojure.core :as c]))

(defonce container (db/pgmq-container))

(defrecord MockAdapter []
  adapter/Adapter
  (execute! [_ _ _] [{:delete true}])
  (query [_ _ _] [{:msg_id 1
                   :read_ct 1
                   :enqueued_at (java.time.Instant/now)
                   :vt (java.time.Instant/now)
                   :message "{\"foo\": \"bar\"}"
                   :headers nil}])
  (with-transaction [_ f] (f))
  (close [_] nil))

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
  (let [adapter (->MockAdapter)
        queue-name "test-queue"]
    (testing "read-message spec validation valid arguments"
      (is (seq? (core/read-message adapter queue-name 30 100 {})))
      (is (seq? (core/read-message adapter queue-name 30 100 {:foo "bar"}))))
    (testing "read-message spec validates invalid adapter"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/read-message did not conform to spec"
                            (core/read-message nil queue-name 10 3 {})))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/read-message did not conform to spec"
                            (core/read-message {} queue-name 10 3 {}))))
    (testing "read-message spec validates invalid queue-name"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/read-message did not conform to spec"
                            (core/read-message adapter 8008 10 3 {})))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/read-message did not conform to spec"
                            (core/read-message adapter nil 10 3 {}))))
    (testing "read-message spec validates invalid visibility_time"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/read-message did not conform to spec"
                            (core/read-message adapter queue-name -1776 3 {})))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/read-message did not conform to spec"
                            (core/read-message adapter queue-name "invalid" 3 {})))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/read-message did not conform to spec"
                            (core/read-message adapter queue-name nil 3 {}))))
    (testing "read-message spec validates invalid quantity"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/read-message did not conform to spec"
                            (core/read-message adapter queue-name 30 -3 {})))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/read-message did not conform to spec"
                            (core/read-message adapter queue-name 30 "invalid" {})))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/read-message did not conform to spec"
                            (core/read-message adapter queue-name 30 nil {}))))
    (testing "read-message spec validates invalid invalid filter"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/read-message did not conform to spec"
                            (core/read-message adapter queue-name 30 3 nil))))))

(deftest send-message-spec-test
  (let [adapter (db/setup-adapter container)
        queue-name "test-queue"]
    (core/create-queue adapter queue-name)
    (testing "send-message spec validation valid arguments"
      (is (s/valid? ::core/msg-id (core/send-message adapter queue-name {:foo "bar"})))
      (is (s/valid? ::core/msg-id (core/send-message adapter queue-name {})))
      (is (s/valid? ::core/msg-id (core/send-message adapter queue-name [])))
      (is (s/valid? ::core/msg-id (core/send-message adapter queue-name 1)))
      (is (s/valid? ::core/msg-id (core/send-message adapter queue-name "some string"))))
    (testing "send-message spec validation invalid adapter"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/send-message did not conform to spec."
                            (core/send-message nil queue-name {:foo "bar"})))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/send-message did not conform to spec."
                            (core/send-message {} queue-name {:foo "bar"}))))
    (testing "send-message spec validation invalid queue-name"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/send-message did not conform to spec."
                            (core/send-message adapter 8008 {})))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/send-message did not conform to spec."
                            (core/send-message adapter nil {}))))
    (testing "send-message spec validation invalid payload"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/send-message did not conform to spec."
                            (core/send-message adapter 8008 nil))))
    (core/drop-queue adapter queue-name)))

(deftest delete-message-spec-test
  (let [adapter (->MockAdapter)
        queue-name "test-queue"]
    (testing "delete-message spec validation with valid arguments"
      (is (s/valid? boolean? (core/delete-message adapter queue-name [100]))))
    (testing "delete-message spec validation with invalid adapter"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/delete-message did not conform to spec."
                            (core/delete-message nil queue-name [100])))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/delete-message did not conform to spec."
                            (core/delete-message {} queue-name [100]))))
    (testing "delete-message spec validation with invalid queue-name"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/delete-message did not conform to spec."
                            (core/delete-message adapter 8008 [100])))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/delete-message did not conform to spec."
                            (core/delete-message adapter nil [100]))))
    (testing "delete-message spec validation with invalid payload"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/delete-message did not conform to spec."
                            (core/delete-message adapter queue-name nil)))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/delete-message did not conform to spec."
                            (core/delete-message adapter queue-name nil)))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/delete-message did not conform to spec."
                            (core/delete-message adapter queue-name ["not an int seq"]))))))

;; TODO: separate these with annotations of more like integration tests and unit tests
;; TODO: Add a delete integration style test
