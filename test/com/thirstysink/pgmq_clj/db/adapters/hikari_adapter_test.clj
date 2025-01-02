(ns com.thirstysink.pgmq-clj.db.adapters.hikari-adapter-test
  (:require [clojure.test :refer [deftest is]]
            [com.thirstysink.pgmq-clj.db.adapter :as adapter]
            [com.thirstysink.pgmq-clj.db.adapters.hikari-adapter :as hikari]))

(def sqlite-config {:jdbc-url "jdbc:sqlite::memory:"})

(deftest sqlite-adapter-basic-test
  (let [adapter (hikari/make-hikari-adapter sqlite-config)
        create-table-sql "CREATE TABLE test_table (id INTEGER PRIMARY KEY, name TEXT);"
        insert-sql "INSERT INTO test_table (name) VALUES (?);"
        select-sql "SELECT * FROM test_table;"]

    ;; Create table
    (adapter/execute! adapter create-table-sql [])

    ;; Insert rows
    (adapter/execute! adapter insert-sql ["Alice"])
    (adapter/execute! adapter insert-sql ["Bob"])

    ;; Query and verify
    (let [results (adapter/query adapter select-sql [])]
      (is (= 2 (count results)))
      (is (= "[\"Alice\"]" (:name (first results))))
      (is (= "[\"Bob\"]" (:name (second results)))))
    (adapter/close adapter)))

(deftest sqlite-adapter-transaction-test
  (let [adapter (hikari/make-hikari-adapter sqlite-config)
        create-table-sql "CREATE TABLE test_table (id INTEGER PRIMARY KEY, name TEXT);"
        insert-sql "INSERT INTO test_table (name) VALUES (?);"
        select-sql "SELECT * FROM test_table;"]

    ;; Create table
    (adapter/execute! adapter create-table-sql [])

    ;; Run a transaction
    (adapter/with-transaction adapter
      (fn [tx]
        (adapter/execute! tx insert-sql ["Alice"])
        (adapter/execute! tx insert-sql ["Bob"])))

    ;; Verify data
    (let [results (adapter/query adapter select-sql [])]
      (is (= 2 (count results)))
      (is (= "[\"Alice\"]" (:name (first results))))
      (is (= "[\"Bob\"]" (:name (second results)))))
    (adapter/close adapter)))

(deftest sqlite-adapter-transaction-rollback-test
  (let [adapter (hikari/make-hikari-adapter sqlite-config)
        create-table-sql "CREATE TABLE test_table (id INTEGER PRIMARY KEY, name TEXT);"
        insert-sql "INSERT INTO test_table (name) VALUES (?);"
        select-sql "SELECT * FROM test_table;"]

    ;; Create table
    (adapter/execute! adapter create-table-sql [])

    ;; Attempt a transaction with a simulated failure
    (try
      (adapter/with-transaction adapter
        (fn [tx]
          (adapter/execute! tx insert-sql ["Alice"])
          (throw (Exception. "Simulated failure"))))
      (catch Exception e
        (is (= "Simulated failure" (.getMessage e)))))

    ;; Verify that no data was committed
    (let [results (adapter/query adapter select-sql [])]
      (is (= 0 (count results))))
    (adapter/close adapter)))
