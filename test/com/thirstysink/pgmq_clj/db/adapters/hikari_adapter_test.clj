(ns com.thirstysink.pgmq-clj.db.adapters.hikari-adapter-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [com.thirstysink.pgmq-clj.db.adapter :as adapter]
            [com.thirstysink.pgmq-clj.db.adapters.hikari-adapter :as hikari])
  (:import [org.testcontainers.containers PostgreSQLContainer]))

;; Create a PostgreSQL container for testing
(defonce container (PostgreSQLContainer. "postgres:17-alpine"))

(defn start-postgres-container []
  (.start container)
  {:jdbc-url (.getJdbcUrl container)
   :username (.getUsername container)
   :password (.getPassword container)})

(defn setup-adapter []
  (hikari/make-hikari-adapter (start-postgres-container)))

(defn stop-postgres-container []
  (.stop container))

(defn reset-table [adapter]
  (let [drop-table-sql "DROP TABLE IF EXISTS test_table;"
        create-table-sql "CREATE TABLE test_table (id SERIAL PRIMARY KEY, name TEXT);"]
    (adapter/execute! adapter drop-table-sql [])
    (adapter/execute! adapter create-table-sql [])))

(use-fixtures :once
  (fn [tests]
    (start-postgres-container)
    (tests)
    (stop-postgres-container)))

(use-fixtures :each
  (fn [tests]
    (let [adapter (setup-adapter)]
      ;; Reset the table before each test
      (reset-table adapter)
      (tests))))

(deftest postgres-adapter-basic-test
  (let [adapter (setup-adapter)
        insert-sql "INSERT INTO test_table (name) VALUES (?);"
        select-sql "SELECT * FROM test_table;"]

    (adapter/execute! adapter insert-sql ["Alice"])
    (adapter/execute! adapter insert-sql ["Bob"])

    (let [results (adapter/query adapter select-sql [])]
      (is (= 2 (count results)))
      (is (= "Alice" (:name (first results))))
      (is (= "Bob" (:name (second results)))))
    (adapter/close adapter)))

(deftest postgres-adapter-transaction-test
  (let [adapter (setup-adapter)
        insert-sql "INSERT INTO test_table (name) VALUES (?);"
        select-sql "SELECT * FROM test_table;"]

    (adapter/with-transaction adapter
      (fn [tx]
        (adapter/execute! tx insert-sql ["Alice"])
        (adapter/execute! tx insert-sql ["Bob"])))

    (let [results (adapter/query adapter select-sql [])]
      (is (= 2 (count results)))
      (is (= "Alice" (:name (first results))))
      (is (= "Bob" (:name (second results)))))
    (adapter/close adapter)))

(deftest postgres-adapter-transaction-rollback-test
  (let [adapter (setup-adapter)
        insert-sql "INSERT INTO test_table (name) VALUES (?);"
        select-sql "SELECT * FROM test_table;"]

    (try
      (adapter/with-transaction adapter
        (fn [tx]
          (adapter/execute! tx insert-sql ["Alice"])
          (throw (Exception. "Simulated failure"))))
      (catch Exception e
        (is (= "Simulated failure" (.getMessage e)))))

    (let [results (adapter/query adapter select-sql [])]
      (is (= 0 (count results))))
    (adapter/close adapter)))
