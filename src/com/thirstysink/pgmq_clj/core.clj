(ns com.thirstysink.pgmq-clj.core
  (:require [cheshire.core :as ches]
            [com.thirstysink.pgmq-clj.db.adapter :as adapter]
            [com.thirstysink.pgmq-clj.instrumentation :as inst]))

(defn create-queue [adapter queue-name]
  (let [create-sql "SELECT pgmq.create(?);"]
    (adapter/execute! adapter create-sql [queue-name])))

(defn drop-queue [adapter queue-name]
  (let [drop-sql "SELECT pgmq.drop_queue(?);"
        result (adapter/execute! adapter drop-sql [queue-name])]
    (:drop-queue result)))

(defn list-queues [adapter]
  (let [list-queues-sql "SELECT * FROM pgmq.list_queues();"
        result (adapter/query adapter list-queues-sql [])]
    result))

(defn send-message [adapter queue-name payload headers delay]
  (let [json-payload (ches/generate-string payload)
        json-headers (ches/generate-string headers)
        send-sql "SELECT * from pgmq.send(?,?::jsonb,?::jsonb, ?::integer);"
        result (adapter/execute! adapter send-sql [queue-name json-payload json-headers delay])]
    (:send result)))

(defn read-message [adapter queue-name visible_time quantity filter]
  (let [json-filter (if (nil? filter)
                      "{}"
                      (ches/generate-string filter))
        read-sql "SELECT * FROM pgmq.read(?,?::integer,?::integer,?::jsonb);"
        result (adapter/query adapter read-sql [queue-name visible_time quantity json-filter])]
    (seq result)))

(defn delete-message [adapter queue-name msg-id]
  (let [delete-sql "SELECT pgmq.delete(?,?);"
        result (adapter/execute! adapter delete-sql [queue-name msg-id])]
    (:delete result)))

(defn pop-message [adapter queue-name]
  (let [pop-sql "SELECT * FROM pgmq.pop(?);"
        result (adapter/query adapter pop-sql [queue-name])]
    (first result)))

(defn archive-message [adapter queue-name msg-ids]
  (let [archive-sql "SELECT * FROM pgmq.archive(?, ?::BIGINT[]);"
        result (adapter/query adapter archive-sql [queue-name (into-array Long msg-ids)])]
    (map :archive result)))

;; TODO: Last thing, make sure query and execute are used as needed
;; delete-batch
;; send-batch
;; create-partitioned
;; read-with-polling

(if inst/instrumentation-enabled?
  (inst/enable-instrumentation)
  (inst/disable-instrumentation))
