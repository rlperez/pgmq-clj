(ns com.thirstysink.pgmq-clj.specs-test
  (:require [com.thirstysink.pgmq-clj.core :as core]
            [com.thirstysink.pgmq-clj.specs :as specs]
            [com.thirstysink.pgmq-clj.instrumentation :as inst]
            [com.thirstysink.pgmq-clj.db.adapter :as adapter]
            [clojure.spec.alpha :as s]
            [clojure.test :refer [deftest is testing use-fixtures]]))

(use-fixtures :once
  (fn [tests]
    (try
      (inst/enable-instrumentation)
      (tests)
      (finally
        (inst/disable-instrumentation)))))

(def sql-time (java.time.Instant/now))

(defrecord MockAdapter []
  adapter/Adapter
  (execute-one! [_ sql _]
    (cond (re-find #"delete" sql)
          {:delete true}
          (re-find #"send_batch" sql)
          [{:send-batch 1 {:send-batch 2} {:send-batch 3}}]
          (re-find #"send" sql)
          {:send 1}))
  (execute! [_ sql _]
    (cond (re-find #"delete" sql)
          [{:delete 1 {:delete 2} {:delete 3}}]))
  (query [_ sql _]
    (cond
      (re-find #"list" sql)
      [{:queue-name "my_queue"
        :is-partitioned false
        :is-unlogged true
        :created-at sql-time}]
      (re-find #"read" sql)
      [{:msg-id 1
        :read-ct 1
        :enqueued-at sql-time
        :vt sql-time
        :message {:foo "bar"}
        :headers nil}]
      (re-find #"pop" sql)
      [{:msg-id 1
        :read-ct 1
        :enqueued-at sql-time
        :vt sql-time
        :message {:foo "bar"}
        :headers nil}]))

  (with-transaction [_ f] (f))
  (close [_] nil))

(deftest json-spec-test
  (testing "json valid values"
    (is (s/valid? ::specs/json 1))
    (is (s/valid? ::specs/json {}))
    (is (s/valid? ::specs/json []))
    (is (s/valid? ::specs/json "A"))
    (is (s/valid? ::specs/json false))
    (is (s/valid? ::specs/json nil))))

(deftest create-queue-name-spec-test
  (let [adapter (->MockAdapter)
        expected-msg #"Call to com.thirstysink.pgmq-clj.core/create-queue did not conform to spec."]
    (testing "create-queue throws exception with an nil adapter"
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           expected-msg
           (core/create-queue nil "test_queue"))))
    (testing "create-queue throws exception with an invalid adapter"
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           expected-msg
           (core/create-queue [] "test_queue"))))
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
           (core/drop-queue nil "test_queue"))))
    (testing "drop-queue throws exception with an invalid adapter"
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           expected-msg
           (core/drop-queue [] "test_queue"))))
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
        queue-name "test_queue"]
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
                            (core/read-message adapter nil 10 3 {})))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/read-message did not conform to spec"
                            (core/read-message adapter "test-queue!" 10 3 {})))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/read-message did not conform to spec"
                            (core/read-message adapter "test-queue" 10 3 {})))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/read-message did not conform to spec"
                            (core/read-message adapter "1__queue" 10 3 {}))))
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
        queue-name "test_queue"]
    (testing "send-message spec validation valid arguments"
      (is (s/valid? ::specs/msg-id (core/send-message adapter queue-name {:data {:foo "bar"} :headers {:baz "bat"}} 0)))
      (is (s/valid? ::specs/msg-id (core/send-message adapter queue-name {:headers {} :data {}} 0)))
      (is (s/valid? ::specs/msg-id (core/send-message adapter queue-name {:data [] :headers {}} 0)))
      (is (s/valid? ::specs/msg-id (core/send-message adapter queue-name {:data 1 :headers {:x-data 2}} 0)))
      (is (s/valid? ::specs/msg-id (core/send-message adapter queue-name {:data "some string" :headers {:x-data "some other string"}} 0))))
    (testing "send-message spec validation invalid adapter"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/send-message did not conform to spec."
                            (core/send-message nil queue-name {:data {:foo "bar"} :headers {}} 0)))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/send-message did not conform to spec."
                            (core/send-message {} queue-name {:data {:foo "bar"} :headers {}} 0))))
    (testing "send-message spec validation invalid queue-name"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/send-message did not conform to spec."
                            (core/send-message adapter 8008 {:data {} :headers {}} 0)))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/send-message did not conform to spec."
                            (core/send-message adapter nil {:data {} :headers {}} 0))))
    (testing "send-message spec validation invalid payload"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/send-message did not conform to spec."
                            (core/send-message adapter 8008 {:headers {}} 0))))
    (testing "send-message spec validation invalid headers"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/send-message did not conform to spec."
                            (core/send-message adapter 8008 {:data {} :headers nil} 0))))
    (testing "send-message spec validation invalid delay"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/send-message did not conform to spec."
                            (core/send-message adapter 8008 {} "Not an int"))))))

(deftest delete-message-spec-test
  (let [adapter (->MockAdapter)
        queue-name "test_queue"]
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
        queue-name "test_queue"]
    (testing "Valid arguments"
      (let [popped-message (core/pop-message adapter queue-name)]
        (is (s/valid? ::specs/message-record popped-message))
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
                                  (core/pop-message nil queue-name)))))))))

(deftest send-message-batch-spec-test
  (let [adapter (->MockAdapter)
        queue-name "_test_queue"]
    (testing "send-message-batch spec validation valid arguments"
      (is (s/valid? ::specs/msg-ids (core/send-message-batch adapter queue-name [{:data {:foo "bar"} :headers {:baz "bat"}}] 0)))
      (is (s/valid? ::specs/msg-ids (core/send-message-batch adapter queue-name [{:data {} :headers {}}] 0)))
      (is (s/valid? ::specs/msg-ids (core/send-message-batch adapter queue-name [{:data [] :headers {}}] 0)))
      (is (s/valid? ::specs/msg-ids (core/send-message-batch adapter queue-name [{:data 1 :headers {:x-data 2}}] 0)))
      (is (s/valid? ::specs/msg-ids (core/send-message-batch adapter queue-name [{:data "some string" :headers {"x-data" "some other string"}}] 0))))
    (testing "send-message-batch spec validation invalid adapter"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/send-message-batch did not conform to spec."
                            (core/send-message-batch nil queue-name [{:data [{:foo "bar"}] :headers {}}] 0)))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/send-message-batch did not conform to spec."
                            (core/send-message-batch {} queue-name [{:data {:foo "bar"} :headers {}}] 0))))
    (testing "send-message-batch spec validation invalid queue-name"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/send-message-batch did not conform to spec."
                            (core/send-message-batch adapter 8008 [{:data [{}] :headers {}}] 0)))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/send-message-batch did not conform to spec."
                            (core/send-message-batch adapter nil [{:data [{}] :headers {}}] 0)))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/send-message-batch did not conform to spec."
                            (core/send-message-batch adapter nil [{:data {} :headers {}}] 0))))
    (testing "send-message-batch spec validation invalid payload"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/send-message-batch did not conform to spec."
                            (core/send-message-batch adapter 8008 nil 0))))
    (testing "send-message-batch spec validation invalid headers"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/send-message-batch did not conform to spec."
                            (core/send-message-batch adapter 8008 [{}] 0))))
    (testing "send-message-batch spec validation invalid delay"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/send-message-batch did not conform to spec."
                            (core/send-message-batch adapter 8008 [{:data {} :headers {}}] "Not an int"))))))

(deftest delete-message-batch-spec-test
  (let [adapter (->MockAdapter)
        queue-name "test_queue_2"]
    (testing "delete-message-batch spec validation with valid arguments"
      (is (s/valid? ::specs/msg-ids (core/delete-message-batch adapter queue-name [100]))))
    (testing "delete-message-batch spec validation with invalid adapter"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/delete-message-batch did not conform to spec."
                            (core/delete-message-batch nil queue-name [100])))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/delete-message-batch did not conform to spec."
                            (core/delete-message-batch {} queue-name [100]))))
    (testing "delete-message-batch spec validation with invalid queue-name"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/delete-message-batch did not conform to spec."
                            (core/delete-message-batch adapter 8008 [100])))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/delete-message-batch did not conform to spec."
                            (core/delete-message-batch adapter nil [100]))))
    (testing "delete-message-batch spec validation with invalid payload"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/delete-message-batch did not conform to spec."
                            (core/delete-message-batch adapter queue-name nil)))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/delete-message-batch did not conform to spec."
                            (core/delete-message-batch adapter queue-name 1)))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/delete-message-batch did not conform to spec."
                            (core/delete-message-batch adapter queue-name [])))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Call to com.thirstysink.pgmq-clj.core/delete-message-batch did not conform to spec."
                            (core/delete-message-batch adapter queue-name "not an int seq"))))))
