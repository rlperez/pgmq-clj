(ns com.thirstysink.pgmq-clj.json
  (:require [cheshire.core :as ches]))

(def ->json
  "Returns a JSON-encoding String for the given Clojure object. Takes an
  optional date format string that Date objects will be encoded with.

  The default date format (in UTC) is: `yyyy-MM-dd'T'HH:mm:ss'Z'`"
  ches/generate-string)
