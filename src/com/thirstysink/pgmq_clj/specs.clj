(ns com.thirstysink.pgmq-clj.specs
  (:require [clojure.spec.alpha :as s]
            [com.thirstysink.pgmq-clj.db.adapter :as adapter]
            [com.thirstysink.pgmq-clj.core :as c]))

(s/def ::adapter #(satisfies? adapter/Adapter %))

(defn- valid-queue-name? [name]
  (and (string? name)
       (not-empty name)
       (re-matches #"^[a-zA-Z_][a-zA-Z0-9_]*$" name)
       (<= (count name) 63)))

(s/def ::queue-name valid-queue-name?)

(s/def ::visibility_time (s/and int? #(>= % 0)))

(s/def ::quantity (s/and int? #(> % 0)))

(s/def ::json
  (s/spec (fn [x] (or (map? x)
                      (vector? x)
                      (string? x)
                      (number? x)
                      (boolean? x)
                      (nil? x)))))

(s/def ::timestamp
  #(instance? java.time.Instant %))

(s/def ::msg-id (s/and number? pos?))

(s/def ::msg-ids (s/coll-of ::msg-id))

(s/def ::non-empty-msg-ids (s/and ::msg-ids (complement empty?)))

(s/def ::read-ct int?)

(s/def ::delay int?)

(s/def ::enqueued-at ::timestamp)

(s/def ::vt ::timestamp)

(s/def ::message ::json)

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
  (s/keys :req-un [::msg-id ::read-ct ::enqueued-at ::vt ::message]
          :opt-un [::headers]))

(s/def ::message-records
  (s/coll-of ::mesage-record))

(s/def ::data ::json)

(s/def ::header-key (s/or :string string? :keyword keyword?))

(s/def ::header-value (s/or :string string? :number number? :list (s/coll-of (s/or :string string? :number number?))))

(s/def ::headers (s/nilable (s/map-of ::header-key ::header-value :min-count 0)))

(s/def ::payload-object
  (s/keys :req-un [::data ::headers]))

(s/def ::payload-objects (s/coll-of ::payload-object))

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
               :payload ::payload-object
               :delay ::delay)
  :ret ::msg-id)

(s/fdef c/read-message
  :args (s/cat :adapter ::adapter
               :queue-name ::queue-name
               :visibility_time ::visibility_time
               :quantity ::quantity
               :filter ::json)
  :ret ::message-records)

(s/fdef c/delete-message
  :args (s/cat :adapter ::adapter
               :queue-name ::queue-name
               :msg-id ::msg-id)
  :ret boolean?)

(s/fdef c/pop-message
  :args (s/cat :adapter ::adapter
               :queue-name ::queue-name)
  :ret ::message-record)

(s/fdef c/archive-messages
  :args (s/cat :adapter ::adapter
               :queue-name ::queue-name
               :msg-ids ::msg-ids)
  :ret ::msg-ids)

(s/fdef c/send-message-batch
  :args (s/cat :adapter ::adapter
               :queue-name ::queue-name
               :payload ::payload-objects
               :delay ::delay)
  :ret ::msg-ids)

(s/fdef c/delete-message-batch
  :args (s/cat :adapter ::adapter
               :queue-name ::queue-name
               :msg-ids ::non-empty-msg-ids)
  :ret ::msg-ids)
