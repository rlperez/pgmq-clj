(ns com.thirstysink.pgmq-clj.db.adapter)

(defprotocol Adapter
  (execute-one! [this sql params] "Execute a `sql` statement and `params` with 0 or 1 return values using `this`.")
  (execute! [this sql params] "Execute a `sql` statement and `params` with 0 or more return values using `this`.")
  (query [this sql params] "Query the database with a given `sql`, `params`, and return results using `this`.")
  (with-transaction [this f] "Wrap a function `f` in a database transaction using `this`.")
  (with-db [this f] "Wrap a function `f` and use the adapter across multiple operations using `this`")
  (close [this] "Performs database connection cleanup using `this`."))
