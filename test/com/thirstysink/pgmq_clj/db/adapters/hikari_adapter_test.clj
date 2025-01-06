(ns com.thirstysink.pgmq-clj.db.adapters.hikari-adapter-test
  (:require [clojure.test :refer [deftest is testing]]
            [next.jdbc :as jdbc]
            [com.thirstysink.pgmq-clj.db.adapter :as adapter]
            [com.thirstysink.pgmq-clj.db.adapters.hikari-adapter :refer :all])
  (:import [com.zaxxer.hikari HikariDataSource]))

(def sqlite-config {:jdbc-url "jdbc:sqlite::memory:"})

(deftest sqlite-adapter-basic-test
  (let [adapter (make-hikari-adapter sqlite-config)
        create-table-sql "CREATE TABLE test_table (id INTEGER PRIMARY KEY, name TEXT);"
        insert-sql "INSERT INTO test_table (name) VALUES (?);"
        select-sql "SELECT * FROM test_table;"]

    (adapter/execute! adapter create-table-sql [])
    (adapter/execute! adapter insert-sql ["Alice"])
    (adapter/execute! adapter insert-sql ["Bob"])

    ;; Query and verify
    (let [results (adapter/query adapter select-sql [])]
      (is (= 2 (count results)))
      (is (= "[\"Alice\"]" (:name (first results))))
      (is (= "[\"Bob\"]" (:name (second results)))))
    (adapter/close adapter)))

(deftest sqlite-adapter-transaction-test
  (let [adapter (make-hikari-adapter sqlite-config)
        create-table-sql "CREATE TABLE test_table (id INTEGER PRIMARY KEY, name TEXT);"
        insert-sql "INSERT INTO test_table (name) VALUES (?);"
        select-sql "SELECT * FROM test_table;"]

    (adapter/execute! adapter create-table-sql [])
    (adapter/with-transaction adapter
      (fn [tx]
        (adapter/execute! tx insert-sql ["Alice"])
        (adapter/execute! tx insert-sql ["Bob"])))

    (let [results (adapter/query adapter select-sql [])]
      (is (= 2 (count results)))
      (is (= "[\"Alice\"]" (:name (first results))))
      (is (= "[\"Bob\"]" (:name (second results)))))
    (adapter/close adapter)))

(deftest sqlite-adapter-transaction-rollback-test
  (let [adapter (make-hikari-adapter sqlite-config)
        create-table-sql "CREATE TABLE test_table (id INTEGER PRIMARY KEY, name TEXT);"
        insert-sql "INSERT INTO test_table (name) VALUES (?);"
        select-sql "SELECT * FROM test_table;"]

    (adapter/execute! adapter create-table-sql [])

    (try
      (adapter/with-transaction adapter
        (fn [tx]
          (adapter/execute! tx insert-sql ["Alice"])
          (throw (Exception. "Simulated failure"))))
      (catch Exception e
        (is (= "Error in transaction" (.getMessage e)))
        (is (= "Simulated failure" (.getMessage (.getCause e))))))

    (let [results (adapter/query adapter select-sql [])]
      (is (= 0 (count results))))
    (adapter/close adapter)))

(deftest execute!-throws-exception
  (testing "HikariAdapter.execute! throws exception"
    (let [mock-datasource (atom nil)
          adapter (->HikariAdapter mock-datasource)]
      (with-redefs [jdbc/execute! (fn [_ _] (throw (Exception. "Mock execute! failure")))]
        (is (thrown-with-msg?
             clojure.lang.ExceptionInfo
             #"Error executing statement"
             (adapter/execute! adapter "UPDATE test SET value = ?" [42])))))))

(deftest query-throws-exception
  (testing "HikariAdapter.query throws exception"
    (let [mock-datasource (atom nil)
          adapter (->HikariAdapter mock-datasource)]
      (with-redefs [jdbc/execute! (fn [_ _ _] (throw (Exception. "Mock query failure")))]
        (is (thrown-with-msg?
             clojure.lang.ExceptionInfo
             #"Error executing query"
             (adapter/query adapter "SELECT * FROM test WHERE id = ?" [1])))))))

(deftest with-transaction-throws-exception
  (testing "HikariAdapter.with-transaction throws exception"
    (let [mock-datasource (atom nil)
          adapter (->HikariAdapter mock-datasource)]
      (with-redefs [jdbc/with-transaction (fn [_ _] (throw (Exception. "Mock transaction failure")))]
        (is (thrown-with-msg?
             clojure.lang.ExceptionInfo
             #"Error in transaction"
             (adapter/with-transaction adapter (fn [tx] (println "Transaction logic")))))))))

(deftest close-throws-exception
  (testing "HikariAdapter.close throws exception"
    ;; Mock the HikariDataSource to throw an exception on `.close`
    (let [mock-datasource (proxy [HikariDataSource] []
                            (close []
                              (throw (Exception. "Mock close failure"))))
          adapter (->HikariAdapter mock-datasource)]
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           #"Failed to close the datasource"
           (adapter/close adapter))))))
