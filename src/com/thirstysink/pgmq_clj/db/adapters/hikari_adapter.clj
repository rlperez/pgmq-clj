(ns com.thirstysink.pgmq-clj.db.adapters.hikari-adapter
  (:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [com.thirstysink.pgmq-clj.db.adapter :as adapter])
  (:import [com.zaxxer.hikari HikariDataSource]))

(defrecord HikariAdapter [datasource]
  adapter/Adapter

  (execute! [this sql params]
    (jdbc/execute! (:datasource this)
                   (if (seq params)
                     [sql params]
                     [sql])))

  (query [this sql params]
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

(defn ensure-pgmq-extension [adapter]
  (let [check-extension-sql "SELECT extname FROM pg_extension WHERE extname = 'pgmq';"]
    (let [extension-check (adapter/query adapter check-extension-sql [])]
      (if (empty? extension-check)
        (throw (ex-info "PGMQ extension is not installed." {:cause :extension-missing}))
        (println "PGMQ extension is installed")))))

(defn ensure-pgmq-extension [adapter]
  (let [check-extension-sql "SELECT extname FROM pg_extension WHERE extname = 'pgmq';"
        extension-check (adapter/query adapter check-extension-sql [])]
    (if (empty? extension-check)
      (throw (ex-info "PGMQ extension is not installed." {:cause :extension-missing}))
      (println "PGMQ extension is installed"))))

(defn make-hikari-adapter [config]
  (let [datasource (doto (HikariDataSource.)
                     (.setJdbcUrl (:jdbc-url config))
                     (.setUsername (:username config))
                     (.setPassword (:password config))
                     (.setMaximumPoolSize (or (:maximum-pool-size config) 4))
                     (.setMinimumIdle (or (:minimum-idle config) 2)))
        adapter (->HikariAdapter datasource)]
    ;; Ensure the pgmq extension is installed
    (ensure-pgmq-extension adapter)
    ;; Return the adapter
    adapter))
