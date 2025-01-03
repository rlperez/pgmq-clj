(ns com.thirstysink.util.db
  (:require [com.thirstysink.pgmq-clj.db.adapters.hikari-adapter :as hikari]))

(defn start-postgres-container [container]
  (.start container)
  {:jdbc-url (.getJdbcUrl container)
   :username (.getUsername container)
   :password (.getPassword container)})

(defn setup-adapter [container]
  (hikari/make-hikari-adapter (start-postgres-container container)))

(defn stop-postgres-container [container]
  (.stop container))
