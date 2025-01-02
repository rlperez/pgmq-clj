(ns com.thirstysink.pgmq-clj.db.adapters.hikari-adapter
  (:require [next.jdbc :as jdbc]
            [com.thirstysink.pgmq-clj.db.adapter :as adapter])
  (:import [com.zaxxer.hikari HikariDataSource]))

(defrecord HikariAdapter [datasource]
  adapter/Adapter

  ;; Provide a connection from the Hikari datasource
  (get-connection [this]
    (.getConnection (:datasource this)))

  ;; Execute a SQL command
  (execute! [this sql params]
    (jdbc/execute! (:datasource this) [sql params]))

  ;; Query the database
  (query [this sql params]
    (jdbc/execute! (:datasource this) [sql params] {:builder-fn jdbc/unqualified-snake-kebab-opts}))

  ;; Run a transaction
  (with-transaction [this f]
    (jdbc/with-transaction [tx (:datasource this)]
      (f tx))))

;; Factory function to create a HikariAdapter
(defn make-hikari-adapter
  [config]
  (let [datasource (doto (HikariDataSource.)
                     (.setJdbcUrl (:jdbc-url config))
                     (.setUsername (:username config))
                     (.setPassword (:password config))
                     (.setMaximumPoolSize (or (:maximum-pool-size config) 4))
                     (.setMinimumIdle (or (:minimum-idle config) 2)))]
    (->HikariAdapter datasource)))
