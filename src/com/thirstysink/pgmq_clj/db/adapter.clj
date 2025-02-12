(ns com.thirstysink.pgmq-clj.db.adapter)

(defprotocol Adapter
  (execute! [this sql params] "Execute a SQL query with parameters.")
  (execute-batch! [this sql params] "Execute a SQL query with a collection paramter")
  (query [this sql params] "Query the database and return results.")
  (with-transaction [this f] "Wrap a function in a database transaction.")
  (close [this] "Performs database connection cleanup."))
