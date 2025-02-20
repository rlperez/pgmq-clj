(ns com.thirstysink.util.db
  (:require [com.thirstysink.pgmq-clj.db.adapters.hikari-adapter :as hikari]
            [com.thirstysink.pgmq-clj.db.adapter :as adapter])
  (:import [org.testcontainers.utility DockerImageName]
           [org.testcontainers.containers PostgreSQLContainer]))

(defn pgmq-container []
  (let [image-name (or (System/getenv "TEST_CONTAINER")
                       "tembo.docker.scarf.sh/tembo/pg17-pgmq:latest")] ;; Fallback if TEST_CONTAINER is not set
    (doto
     (PostgreSQLContainer.
      (-> (DockerImageName/parse image-name)
          (.asCompatibleSubstituteFor "postgres")))
      (.withInitScript "sql/init.sql"))))

(defn start-postgres-container [^PostgreSQLContainer container]
  (.start container)
  {:jdbc-url (.getJdbcUrl container)
   :username (.getUsername container)
   :password (.getPassword container)})

(defn setup-adapter [^PostgreSQLContainer container]
  (hikari/make-hikari-adapter (start-postgres-container container)))

(defn stop-postgres-container [^PostgreSQLContainer container]
  (.stop container))

(defn reset-table [adapter table_name]
  (let [drop-table-sql (format "DROP TABLE IF EXISTS %s;" table_name)
        create-table-sql (format "CREATE TABLE %s (id SERIAL PRIMARY KEY, name TEXT);" table_name)]
    (adapter/execute-one! adapter drop-table-sql [])
    (adapter/execute-one! adapter create-table-sql [])))
