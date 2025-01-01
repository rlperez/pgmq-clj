(ns com.thirstysink.pgmq-clj.db.adapter)

(defprotocol Adapter
  (get-connection [this] "Returns a database connection. Typically used in a transaction or query context.")
  (execute! [this sql params] "Execute a SQL query with parameters.")
  (query [this sql params] "Query the database and return results.")
  (with-transaction [this f] "Wrap a function in a database transaction."))
