(ns com.thirstysink.pgmq-clj.db.adapter)

(defprotocol Adapter
  (execute-one! [this sql params] "Execute a SQL statement with 0 or 1 return values.")
  (execute! [this sql params] "Execute a SQL statement with 0 or more return values.")
  (query [this sql params] "Query the database and return results.")
  (with-transaction [this f] "Wrap a function in a database transaction.")
  (close [this] "Performs database connection cleanup."))
