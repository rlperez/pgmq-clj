(ns com.thirstysink.pgmq-clj.db.adapters.hikari-adapter
  (:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [com.thirstysink.pgmq-clj.db.adapter :as adapter])
  (:import [com.zaxxer.hikari HikariDataSource]))

(defrecord HikariAdapter [datasource]
  adapter/Adapter

  (execute! [this sql params]
    (println "Executing SQL:" sql "with params:" params)
    (jdbc/execute! (:datasource this)
                   (if (seq params)
                     [sql params]
                     [sql])))

  (query [this sql params]
    (println "Querying SQL:" sql "with params:" params)
    (jdbc/execute! (:datasource this)
                   (if (seq params)
                     [sql params]
                     [sql])
                   {:builder-fn rs/as-unqualified-lower-maps}))

  (with-transaction [this f]
    (jdbc/with-transaction [tx (:datasource this)]
      (f (assoc this :datasource tx))))

  (close [this]
    (.close (:datasource this))))

(defn make-hikari-adapter [config]
  (let [datasource (doto (HikariDataSource.)
                     (.setJdbcUrl (:jdbc-url config))
                     (.setUsername (:username config))
                     (.setPassword (:password config))
                     (.setMaximumPoolSize (or (:maximum-pool-size config) 4))
                     (.setMinimumIdle (or (:minimum-idle config) 2)))]
    (->HikariAdapter datasource)))
