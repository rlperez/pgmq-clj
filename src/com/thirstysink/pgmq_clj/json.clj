(ns com.thirstysink.pgmq-clj.json
  (:require [cheshire.core :as ches]))

(def ->json ches/generate-string)
