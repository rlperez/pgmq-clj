(ns com.thirstysink.pgmq-clj.db.adapters.hikari-adapter
  (:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [com.thirstysink.pgmq-clj.db.adapter :as adapter])
  (:import [com.zaxxer.hikari HikariDataSource]))

(defrecord HikariAdapter [datasource]
  adapter/Adapter

  (execute! [this sql params]
    (try
      (jdbc/execute! (:datasource this)
                     (cond-> sql
                       (seq params) (jdbc/with-parameters params)))
      (catch Exception e
        (throw (ex-info "Failed to execute SQL statement"
                        {:type ::execute-error
                         :sql sql
                         :params params}
                        e)))))

  (query [this sql params]
    (try
      (jdbc/execute! (:datasource this)
                     (cond-> sql
                       (seq params) (jdbc/with-parameters params))
                     {:builder-fn rs/as-unqualified-lower-maps})
      (catch Exception e
        (throw (ex-info "Failed to execute query"
                        {:type ::query-error
                         :sql sql
                         :params params}
                        e)))))

  (with-transaction [this f]
    (try
      (jdbc/with-transaction [tx (:datasource this)]
        (f (assoc this :datasource tx)))
      (catch Exception e
        (throw (ex-info "Failed to execute within a transaction"
                        {:type ::transaction-error}
                        e)))))

  (close [this]
    (try
      (.close (:datasource this))
      (catch Exception e
        (throw (ex-info "Failed to close the datasource"
                        {:type ::close-error}
                        e))))))

(defn make-hikari-adapter [config]
  (try
    (let [datasource (doto (HikariDataSource.)
                       (.setJdbcUrl (:jdbc-url config))
                       (.setUsername (:username config))
                       (.setPassword (:password config))
                       (.setMaximumPoolSize (or (:maximum-pool-size config) 4))
                       (.setMinimumIdle (or (:minimum-idle config) 2)))]
      (->HikariAdapter datasource))
    (catch Exception e
      (throw (ex-info "Failed to create HikariAdapter"
                      {:type ::adapter-creation-error
                       :config config}
                      e)))))
