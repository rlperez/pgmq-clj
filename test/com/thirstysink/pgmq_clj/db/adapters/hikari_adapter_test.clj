(ns com.thirstysink.pgmq-clj.db.adapters.hikari-adapter-test
  (:require [next.jdbc :as jdbc]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [com.thirstysink.pgmq-clj.db.adapter :as adapter]
            [com.thirstysink.pgmq-clj.db.adapters.hikari-adapter :refer [->HikariAdapter]]
            [com.thirstysink.util.db :as db])
  (:import [com.zaxxer.hikari HikariDataSource]))

(defonce container (db/pgmq-container))

(defn- reset-table [adapter]
  (let [drop-table-sql "DROP TABLE IF EXISTS test_table;"
        create-table-sql "CREATE TABLE test_table (id SERIAL PRIMARY KEY, name TEXT);"]
    (adapter/execute! adapter drop-table-sql [])
    (adapter/execute! adapter create-table-sql [])))

(use-fixtures :once
  (fn [tests]
    (db/start-postgres-container container)
    (try
      (tests)
      (finally
        (db/stop-postgres-container container)))))

(deftest postgres-adapter-basic-test
  (let [adapter (db/setup-adapter container)
        insert-sql "INSERT INTO test_table (name) VALUES (?);"
        select-sql "SELECT * FROM test_table;"]
    (testing "execute! and query execute an insert and query."
      (reset-table adapter)
      (adapter/execute! adapter insert-sql ["Alice"])
      (adapter/execute! adapter insert-sql ["Bob"])

      (let [results (adapter/query adapter select-sql [])]
        (is (= 2 (count results)))
        (is (= "Alice" (:name (first results))))
        (is (= "Bob" (:name (second results))))))

    (testing "execute! and query execute an insert and query wrapped in transaction."
      (reset-table adapter)
      (adapter/with-transaction adapter
        (fn [tx]
          (adapter/execute! tx insert-sql ["Alice"])
          (adapter/execute! tx insert-sql ["Bob"])))

      (let [results (adapter/query adapter select-sql [])]
        (is (= 2 (count results)))
        (is (= "Alice" (:name (first results))))
        (is (= "Bob" (:name (second results))))))
    (testing "with-transaction wrapper performs a rollback when failed insert occurs."
      (try
        (reset-table adapter)
        (adapter/with-transaction adapter
          (fn [tx]
            (adapter/execute! tx insert-sql ["Alice"])
            (throw (Exception. "Simulated failure"))))
        (catch Exception e
          (is (= "Error in transaction" (.getMessage e)))
          (is (= "Simulated failure" (.getMessage (.getCause e))))))

      (let [results (adapter/query adapter select-sql [])]
        (is (= 0 (count results)))))
    (adapter/close adapter)))

(deftest throws-handling-test
  (testing "HikariAdapter.execute! throws exception"
    (let [mock-datasource (atom nil)
          adapter (->HikariAdapter mock-datasource)]
      (with-redefs [jdbc/execute! (fn [_ _] (throw (Exception. "Mock execute! failure")))]
        (is (thrown-with-msg?
             clojure.lang.ExceptionInfo
             #"Error executing statement"
             (adapter/execute! adapter "UPDATE test SET value = ?" [42]))))))
  (testing "HikariAdapter.query throws exception"
    (let [mock-datasource (atom nil)
          adapter (->HikariAdapter mock-datasource)]
      (with-redefs [jdbc/execute! (fn [_ _ _] (throw (Exception. "Mock query failure")))]
        (is (thrown-with-msg?
             clojure.lang.ExceptionInfo
             #"Error executing query"
             (adapter/query adapter "SELECT * FROM test WHERE id = ?" [1]))))))
  (testing "HikariAdapter.with-transaction throws exception"
    (let [mock-datasource (atom nil)
          adapter (->HikariAdapter mock-datasource)]
      (with-redefs [jdbc/with-transaction (fn [_ _] (throw (Exception. "Mock transaction failure")))]
        (is (thrown-with-msg?
             clojure.lang.ExceptionInfo
             #"Error in transaction"
             (adapter/with-transaction adapter (fn [_tx] (println "Transaction logic"))))))))
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
