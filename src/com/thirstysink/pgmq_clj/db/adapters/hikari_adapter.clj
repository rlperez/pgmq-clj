(ns com.thirstysink.pgmq-clj.db.adapters.hikari-adapter
  (:require [next.jdbc :as jdbc]
            [next.jdbc.date-time]
            [next.jdbc.result-set :as rs]
            [next.jdbc.prepare :as prepare]
            [com.thirstysink.pgmq-clj.db.adapter :as adapter]
            [com.thirstysink.pgmq-clj.json :refer [<-json ->json]])
  (:import [com.zaxxer.hikari HikariDataSource]
           [org.postgresql.util PGobject]
           [java.sql PreparedStatement]))

(defrecord HikariAdapter [datasource]
  adapter/Adapter

  ;; Execute a query (e.g., UPDATE, INSERT, DELETE) with optional parameters
  (execute-one! [this sql params]
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

  (execute! [this sql params]
    (try
      (jdbc/execute!
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

(defn ->pgobject
  "Transforms Clojure data to a PGobject that contains the data as
  JSON. PGObject type defaults to `jsonb` but can be changed via
  metadata key `:pgtype`"
  [x]
  (let [pgtype (or (:pgtype (meta x)) "jsonb")]
    (doto (PGobject.)
      (.setType pgtype)
      (.setValue (->json x)))))

(defn <-pgobject
  "Transform PGobject containing `json` or `jsonb` value to Clojure data."
  [^PGobject v]
  (let [type  (.getType v)
        value (.getValue v)]
    (if (#{"jsonb" "json"} type)
      (some-> value <-json (with-meta {:pgtype type}))
      value)))

(extend-protocol prepare/SettableParameter
  clojure.lang.IPersistentMap
  (set-parameter [m ^PreparedStatement s i]
    (.setObject s i (->pgobject m)))
  clojure.lang.IPersistentVector
  (set-parameter [v ^PreparedStatement s i]
    (.setObject s i (->pgobject v))))

;; if a row contains a PGobject then we'll convert them to Clojure data
;; while reading (if column is either "json" or "jsonb" type):
(extend-protocol rs/ReadableColumn
  org.postgresql.util.PGobject
  (read-column-by-label [^org.postgresql.util.PGobject v _]
    (<-pgobject v))
  (read-column-by-index [^org.postgresql.util.PGobject v _2 _3]
    (<-pgobject v))
  java.sql.Timestamp
  (read-column-by-label [^java.sql.Timestamp v _]
    (.toInstant v))
  (read-column-by-index [^java.sql.Timestamp v _2 _3]
    (.toInstant v)))

(defn ensure-pgmq-extension [adapter]
  (let [check-extension-sql "SELECT extname FROM pg_extension WHERE extname = 'pgmq';"
        extension-check (adapter/query adapter check-extension-sql [])]
    (when (empty? extension-check)
      (throw (ex-info "PGMQ extension is not installed." {:cause :extension-missing})))))

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
