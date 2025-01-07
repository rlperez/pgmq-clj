(ns com.thirstysink.pgmq-clj.core
  (:require [clojure.spec.alpha :as s]
            [com.thirstysink.pgmq-clj.db.adapter :as adapter]
            [com.thirstysink.pgmq-clj.instrumentation :as inst]))

(s/def ::adapter #(satisfies? adapter/Adapter %))

(s/def ::queue-name (s/and string? not-empty))

(s/fdef create-queue
  :args (s/cat :adapter ::adapter :queue-name ::queue-name)
  :ret nil)

(defn create-queue [adapter queue-name]
  (let [create-sql "SELECT pgmq.create(?);"]
    (adapter/execute! adapter create-sql [queue-name])))

(s/fdef drop-queue
  :args (s/cat :adapter ::adapter :queue-name ::queue-name)
  :ret boolean?)

(defn drop-queue [adapter queue-name]
  (let [drop-sql "SELECT pgmq.drop_queue(?);"
        result (adapter/execute! adapter drop-sql [queue-name])]
    (get-in (first result) [:drop_queue])))

(defn send-message [adapter queue-name payload] nil)

(defn read-message [adapter queue-name] nil)

(defn pop-message [adapter queue-name] nil)

(defn delete-message [adapter queue-name msg-id] nil)

(defn archive-message [adapter queue-name msg-id] nil)

(if inst/instrumentation-enabled?
  (inst/enable-instrumentation `com.thirstysink.pgmq-clj.core)
  (inst/disable-instrumentation `com.thirstysink.pgmq-clj.core))
