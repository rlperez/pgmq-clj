(ns com.thirstysink.pgmq-clj.db.adapters.hikari-adapter
  (:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [com.thirstysink.pgmq-clj.db.adapter :as adapter])
  (:import [com.zaxxer.hikari HikariDataSource]))

(defrecord HikariAdapter [datasource]
  adapter/Adapter

  ;; Execute a query (e.g., UPDATE, INSERT, DELETE) with optional parameters
  (execute! [this sql params]
    (try
      (jdbc/execute-one!
       (:datasource this)
       (if (seq params)
         (into [sql] params)
         [sql])
       {:builder-fn rs/as-unqualified-kebab-maps})
      (catch Exception e
        (throw (ex-info "Error executing statement"
                        {:type :execute-error
                         :sql sql
                         :params params}
                        e)))))

  ;; Execute a SELECT query with optional parameters
  (query [this sql params]
    (try
      (jdbc/execute!
       (:datasource this)
       (if (seq params)
         (into [sql] params)
         [sql])
       {:builder-fn rs/as-unqualified-kebab-maps})
      (catch Exception e
        (throw (ex-info "Error executing query"
                        {:type :query-error
                         :sql sql
                         :params params}
                        e)))))

  ;; Perform a transactional operation
  (with-transaction [this f]
    (try
      (jdbc/with-transaction [tx (:datasource this)]
        (f (assoc this :datasource tx)))
      (catch Exception e
        (throw (ex-info "Error in transaction"
                        {:type :transaction-error}
                        e)))))

  ;; Close the connection pool
  (close [this]
    (try
      (.close (:datasource this))
      (catch Exception e
        (throw (ex-info "Failed to close the datasource"
                        {:type ::close-error}
                        e))))))

(defn ensure-pgmq-extension [adapter]
  (let [check-extension-sql "SELECT extname FROM pg_extension WHERE extname = 'pgmq';"
        extension-check (adapter/query adapter check-extension-sql [])]
    (when (empty? extension-check)
      (throw (ex-info "PGMQ extension is not installed." {:cause :extension-missing})))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn make-hikari-adapter [config]
  (let [datasource (doto (HikariDataSource.)
                     (.setJdbcUrl (:jdbc-url config))
                     (.setUsername (:username config))
                     (.setPassword (:password config))
                     (.setMaximumPoolSize (or (:maximum-pool-size config) 4))
                     (.setMinimumIdle (or (:minimum-idle config) 2)))
        adapter (->HikariAdapter datasource)]
    (ensure-pgmq-extension adapter)
    adapter))
