(ns com.thirstysink.pgmq-clj.core-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [com.thirstysink.pgmq-clj.core :as core]
            [com.thirstysink.pgmq-clj.db.adapter :as adapter]
            [com.thirstysink.util.db :as db])
  (:import [org.testcontainers.containers PostgreSQLContainer]))

(defonce container (PostgreSQLContainer. "postgres:17-alpine"))

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
