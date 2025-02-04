(ns com.thirstysink.pgmq-clj.specs-test
  (:require [com.thirstysink.pgmq-clj.core :as core]
            [com.thirstysink.pgmq-clj.specs :as specs]
            [com.thirstysink.pgmq-clj.instrumentation :as inst]
            [com.thirstysink.pgmq-clj.db.adapter :as adapter]
            [clojure.spec.alpha :as s]
            [clojure.test :refer [deftest is testing use-fixtures]])
  (:import [java.time Instant]))

(use-fixtures :once
  (fn [tests]
    (try
      (inst/enable-instrumentation)
      (tests)
      (finally
        (inst/disable-instrumentation)))))

(defrecord MockAdapter []
  adapter/Adapter
  (execute! [_ sql _]
    (cond (re-find #"delete" sql)
          {:delete true}
          (re-find #"send" sql)
          {:send 1}))
  (query [_ sql _]
    (cond
      (re-find #"list" sql)
      [{:queue-name "my-queue"
        :is-partitioned false
        :is-unlogged true
        :created-at (Instant/now)}]
      (re-find #"read" sql)
      [{:msg-id 1
        :read-ct 1
        :enqueued-at (Instant/now)
        :vt (Instant/now)
        :message {:foo "bar"}
        :headers nil}]
      (re-find #"pop" sql)
      [{:msg-id 1
        :read-ct 1
        :enqueued-at (Instant/now)
        :vt (Instant/now)
        :message {:foo "bar"}
        :headers nil}]))

  (with-transaction [_ f] (f))
  (close [_] nil))

(deftest timestamp-spec-test
  (testing "Valid string formats"
    (is (s/valid? ::specs/timestamp "2025-01-11T14:30:00"))
    (is (s/valid? ::specs/timestamp "2025-01-11T14:30:00.123456"))
    (is (s/valid? ::specs/timestamp "2025-01-11T14:30:00.000000"))
    (is (s/valid? ::specs/timestamp "2025-01-11T14:30:00+00:00"))
    (is (s/valid? ::specs/timestamp (Instant/now))))
  (testing "Invalid string formats"
    (is (not (s/valid? ::specs/timestamp "11/01/2025 14:30:00")))
    (is (not (s/valid? ::specs/timestamp "2025-01-11 14:60:00")))
    (is (not (s/valid? ::specs/timestamp "2025-01-11T14:60:00")))
    (is (not (s/valid? ::specs/timestamp "2025-01-11T14:30:00+25:00")))
    (is (not (s/valid? ::specs/timestamp "2025-01-11T14:30:00:00")))
    (is (not (s/valid? ::specs/timestamp "not an instant"))))
  (testing "Instant object"
    (is (s/valid? ::specs/timestamp (java.time.Instant/now))))
  (testing "Invalid Instant object"
    (is (not (s/valid? ::specs/timestamp "not an instant")))))

(deftest create-queue-name-spec-test
  (let [adapter (->MockAdapter)
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

(deftest drop-queue-name-spec-test
  (let [adapter (->MockAdapter)
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
                            (core/read-message adapter queue-name 30 nil {}))))))

(deftest send-message-spec-test
  (let [adapter (->MockAdapter)
        queue-name "test-queue"]
    (core/create-queue adapter queue-name)
    (testing "send-message spec validation valid arguments"
      (is (s/valid? ::specs/msg-id (core/send-message adapter queue-name {:foo "bar"} {:baz "bat"} 0)))
      (is (s/valid? ::specs/msg-id (core/send-message adapter queue-name {} {} 0)))
      (is (s/valid? ::specs/msg-id (core/send-message adapter queue-name [] [] 0)))
      (is (s/valid? ::specs/msg-id (core/send-message adapter queue-name 1 2 0)))
      (is (s/valid? ::specs/msg-id (core/send-message adapter queue-name "some string" "some other string" 0))))
    (testing "send-message spec validation invalid adapter"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/send-message did not conform to spec."
                            (core/send-message nil queue-name {:foo "bar"} {} 0)))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/send-message did not conform to spec."
                            (core/send-message {} queue-name {:foo "bar"} {} 0))))
    (testing "send-message spec validation invalid queue-name"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/send-message did not conform to spec."
                            (core/send-message adapter 8008 {} {} 0)))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/send-message did not conform to spec."
                            (core/send-message adapter nil {} {} 0))))
    (testing "send-message spec validation invalid payload"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/send-message did not conform to spec."
                            (core/send-message adapter 8008 nil {} 0))))
    (testing "send-message spec validation invalid headers"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/send-message did not conform to spec."
                            (core/send-message adapter 8008 {} nil 0))))
    (testing "send-message spec validation invalid delay"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/send-message did not conform to spec."
                            (core/send-message adapter 8008 {} {} "Not an int"))))

    (core/drop-queue adapter queue-name)))

(deftest delete-message-spec-test
  (let [adapter (->MockAdapter)
        queue-name "test-queue"]
    (testing "delete-message spec validation with valid arguments"
      (is (s/valid? boolean? (core/delete-message adapter queue-name 100))))
    (testing "delete-message spec validation with invalid adapter"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/delete-message did not conform to spec."
                            (core/delete-message nil queue-name 100)))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/delete-message did not conform to spec."
                            (core/delete-message {} queue-name 100))))
    (testing "delete-message spec validation with invalid queue-name"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/delete-message did not conform to spec."
                            (core/delete-message adapter 8008 100)))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/delete-message did not conform to spec."
                            (core/delete-message adapter nil 100))))
    (testing "delete-message spec validation with invalid payload"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/delete-message did not conform to spec."
                            (core/delete-message adapter queue-name nil)))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/delete-message did not conform to spec."
                            (core/delete-message adapter queue-name "not an int seq"))))))

(deftest list-queues-spec-test
  (let [adapter (->MockAdapter)]
    (testing "list-queues spec validation with valid arguments"
      (is (s/valid? ::specs/queue-result (core/list-queues adapter))))
    (testing "list-queues spec validation with invalid adapter"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/list-queues did not conform to spec."
                            (core/list-queues nil))))))

(deftest pop-message-test
  (let [adapter (->MockAdapter)
        queue-name "test-queue"]
    (testing "Valid arguments"
      (is (s/valid? ::specs/message-record (core/pop-message adapter queue-name)))
      (testing "Invalid arguments"
        (let [msg #"Call to com.thirstysink.pgmq-clj.core/pop-message did not conform to spec."]
          (is (thrown-with-msg? clojure.lang.ExceptionInfo
                                msg
                                (core/pop-message nil queue-name)))
          (is (thrown-with-msg? clojure.lang.ExceptionInfo
                                msg
                                (core/pop-message {} queue-name)))
          (is (thrown-with-msg? clojure.lang.ExceptionInfo
                                msg
                                (core/pop-message nil queue-name))))))))
