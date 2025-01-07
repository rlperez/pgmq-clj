(ns com.thirstysink.pgmq-clj.core-test
  (:require [clojure.test :refer [deftest is use-fixtures testing]]
            [com.thirstysink.pgmq-clj.core :as core]
            [com.thirstysink.pgmq-clj.db.adapter :as adapter]
            [com.thirstysink.util.db :as db]))

(defonce container (db/pgmq-container))

(use-fixtures :once
  (fn [tests]
    (db/start-postgres-container container)
    (tests)
    (db/stop-postgres-container container)))

(deftest create-and-drop-queue-test
  (let [adapter (db/setup-adapter container)
        queue-name "test_queue"]

    (core/create-queue adapter queue-name)

    (let [result (adapter/query adapter "SELECT * FROM pgmq.list_queues() WHERE queue_name = ?;" [queue-name])]
      (is (= 1 (count result)))
      (is (= queue-name (:queue_name (first result)))))

    (core/drop-queue adapter queue-name)

    (let [result (adapter/query adapter "SELECT * FROM pgmq.list_queues() WHERE queue_name = ?;" [queue-name])]
      (is (empty? result)))))

(deftest create-queue-name-test
  (testing "create-queue throws exception with an empty queue-name"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"Queue name must be a non-empty string"
         (core/create-queue nil ""))))
  (testing "create-queue throws exception with a nil queue-name"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"Queue name must be a non-empty string"
         (core/create-queue nil nil))))
  (testing "create-queue throws exception with a non string queue-name"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"Queue name must be a non-empty string"
         (core/create-queue nil 1)))))

(deftest drop-queue-name-test
  (testing "drop-queue throws exception with an empty queue-name"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"Queue name must be a non-empty string"
         (core/drop-queue nil ""))))
  (testing "drop-queue throws exception with a nil queue-name"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"Queue name must be a non-empty string"
         (core/drop-queue nil nil))))
  (testing "drop-queue throws exception with a non string queue-name"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"Queue name must be a non-empty string"
         (core/drop-queue nil 1)))))
