(ns com.thirstysink.pgmq-clj.specs
  (:require [clojure.spec.alpha :as s]
            [com.thirstysink.pgmq-clj.db.adapter :as adapter]
            [com.thirstysink.pgmq-clj.core :as c])
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

(s/def ::delay int?)

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

(s/fdef c/create-queue
  :args (s/cat :adapter ::adapter :queue-name ::queue-name)
  :ret nil)

(s/fdef c/drop-queue
  :args (s/cat :adapter ::adapter :queue-name ::queue-name)
  :ret boolean?)

(s/fdef c/list-queues
  :args (s/cat :adapter ::adapter)
  :ret ::queue-result)

(s/fdef c/send-message
  :args (s/cat :adapter ::adapter
               :queue-name ::queue-name
               :payload ::json
               :headers ::json
               :delay ::delay)
  :ret ::msg-id)

(s/fdef c/read-message
  :args (s/cat :adapter ::adapter
               :queue-name ::queue-name
               :visibility_time ::visibility_time
               :quantity ::quantity
               :filter ::json)
  :ret ::message-result)

(s/fdef c/delete-message
  :args (s/cat :adapter ::adapter
               :queue-name ::queue-name
               :msg-id ::msg-id)
  :ret boolean?)

(s/fdef c/pop-message
  :args (s/cat :adapter ::adapter
               :queue-name ::queue-name)
  :ret ::queue-record)
