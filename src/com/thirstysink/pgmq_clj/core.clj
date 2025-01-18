(ns com.thirstysink.pgmq-clj.core
  (:require [clojure.spec.alpha :as s]
            [cheshire.core :as ches]
            [com.thirstysink.pgmq-clj.db.adapter :as adapter]
            [com.thirstysink.pgmq-clj.instrumentation :as inst])
  (:import [java.time.format DateTimeFormatter]
           java.time.Instant))

(s/def ::adapter #(satisfies? adapter/Adapter %))

(s/def ::queue-name (s/and string? not-empty))

(s/def ::visibility_time (s/and int? #(>= % 0)))

(s/def ::quantity (s/and int? #(> % 0)))

(s/def ::json
  (s/or :map map?
        :vector vector?
        :string string?
        :number number?
        :boolean boolean?
        :nil? nil?))

(s/def ::timestamp
  (s/or :string (s/and string?
                       #(try
                          (.parse DateTimeFormatter/ISO_DATE_TIME %)
                          true
                          (catch Exception _ false)))
        :instant #(instance? Instant %)))

(s/def ::msg-id (s/and number? pos?))

(s/def ::msg-ids
  (s/and (s/coll-of ::msg-id) #(seq %)))

(s/def ::read-ct int?)

(s/def ::enqueued-at ::timestamp)

(s/def ::vt ::timestamp)

(s/def ::message ::json)

(s/def ::headers ::json)

(s/def ::is-partitioned boolean?)

(s/def ::is-unlogged boolean?)

(s/def ::created-at (fn [x] #(instance? java.time.Instant x)))

(s/def ::queue-record
  (s/keys :req-un [::queue-name
                   ::is-partitioned
                   ::is-unlogged
                   ::created-at]))

(s/def ::queue-result (s/coll-of ::queue-record))

(s/def ::message-record
  (s/keys :req-un [::msg-id ::read-ct ::enqueued-at ::vt ::message ::headers]))

(s/def ::message-result (s/coll-of ::message-record))

(s/fdef create-queue
  :args (s/cat :adapter ::adapter :queue-name ::queue-name)
  :ret nil)

(s/fdef drop-queue
  :args (s/cat :adapter ::adapter :queue-name ::queue-name)
  :ret boolean?)

(s/fdef list-queues
  :args (s/cat :adapter ::adapter)
  :ret ::queue-result)

(s/fdef send-message
  :args (s/cat :adapter ::adapter
               :queue-name ::queue-name
               :payload ::json)
  :ret int?)

(s/fdef read-message
  :args (s/cat :adapter ::adapter
               :queue-name ::queue-name
               :visibility_time ::visibility_time
               :quantity ::quantity
               :filter ::json)
  :ret ::message-result)

(s/fdef delete-message
  :args (s/cat :adapter ::adapter
               :queue-name ::queue-name
               :msg-id ::msg-id)
  :ret boolean?)

(defn create-queue [adapter queue-name]
  (let [create-sql "SELECT pgmq.create(?);"]
    (adapter/execute! adapter create-sql [queue-name])))

(defn drop-queue [adapter queue-name]
  (let [drop-sql "SELECT pgmq.drop_queue(?);"
        result (adapter/query adapter drop-sql [queue-name])]
    (get-in (first result) [:drop_queue])))

(defn list-queues [adapter]
  (let [list-queues-sql "SELECT * FROM pgmq.list_queues();"]
    (adapter/query adapter list-queues-sql [])))

;; TODO: I need to add at a minimum delay
;; TODO: Add headers as an optional field.
(defn send-message [adapter queue-name payload]
  (let [json-payload (ches/generate-string payload)
        send-sql "SELECT * from pgmq.send(?,?::jsonb);"
        result (adapter/query adapter send-sql [queue-name json-payload])]
    (get-in (first result) [:send])))

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
    (get-in (first result) [:delete])))

;; TODO: (defn pop-message [adapter queue-name] nil)

;; TODO: (defn archive-message [adapter queue-name msg-id] nil)

;; TODO:
;; delete-batch
;; send-batch
; create-partitioned
;; read-with-polling

(if inst/instrumentation-enabled?
  (inst/enable-instrumentation)
  (inst/disable-instrumentation))
