(ns com.thirstysink.pgmq-clj.core
  (:require [clojure.string :as str]
            [cheshire.core :as ches]
            [com.thirstysink.pgmq-clj.db.adapter :as adapter]
            [com.thirstysink.pgmq-clj.instrumentation :as inst]))

(set! *warn-on-reflection* true)

(defn create-queue
  "Create a queue named `queue-name` using a given `adapter`.

  Example:
  ```clojure
  (core/create-queue adapter \"test-queue\")
  ;; => nil
  ```"
  [adapter queue-name]
  (let [create-sql "SELECT pgmq.create(?);"]
    (adapter/execute-one! adapter create-sql [queue-name]))
  nil)

(defn drop-queue
  "Drop queue named `queue-name` using a given `adapter`.

  Example:
  ```clojure
  (core/drop-queue adapter \"test-queue-2\")
  ;; => true
  ```"
  [adapter queue-name]
  (let [drop-sql "SELECT pgmq.drop_queue(?);"
        result (adapter/execute-one! adapter drop-sql [queue-name])]
    (:drop-queue result)))

(defn purge-queue
  "Purge queue named `queue-name` contents using a given `adapter` then
   and return count of purged items.

   Example:
   ```clojure
   (c/purge-queue adapter queue-name)
   ;; => 2
   ```"
  [adapter queue-name]
  (let [purge-sql "SELECT pgmq.purge_queue(?);"
        result (adapter/execute-one! adapter purge-sql [queue-name])]
    (:purge-queue result)))

(defn list-queues
  "List all queues using a given `adapter`.
  Example:
  (core/list-queues adapter)
  ;; => [{:queue-name \"test-queue\",
    :is-partitioned false,
    :is-unlogged false,
    :created-at
    #object[java.time.Instant 0x680b0f16 \"2025-03-20T01:01:42.842248Z\"]}
   {:queue-name \"test-queue-2\",
    :is-partitioned false,
    :is-unlogged false,
    :created-at
    #object[java.time.Instant 0x45e79bdf \"2025-03-20T01:01:46.292274Z\"]}
   {:queue-name \"test-queue-3\",
    :is-partitioned false,
    :is-unlogged false,
    :created-at
    #object[java.time.Instant 0x19767429 \"2025-03-20T01:01:54.665295Z\"]}]"
  [adapter]
  (let [list-queues-sql "SELECT * FROM pgmq.list_queues();"
        result (adapter/query adapter list-queues-sql [])]
    result))

(defn send-message
  "Send one message to a queue `queue-name` with a `payload`
  that will not be read for `delay` seconds using a given `adapter`.
  A `delay` of 0 indicates it may be read immediately.

  Example Payloads:
  - `{:data {:foo \"bad\"} :headers {:x-data \"baz\"}}`
  - `{:data \"feed\" :headers {:version \"3\"}}`

  Example:
  (core/send-message adapter \"test-queue\" {:data {:order-count 12 :user-id \"0f83fbeb-345b-41ca-bbec-3bace0cff5b4\"} :headers {:TENANT \"b5bda77b-8283-4a6d-8de8-40a5041a60ee\"}} 90)
  ;; => 1"
  [adapter queue-name payload delay]
  (let [json-payload (ches/generate-string (:data payload))
        json-headers (ches/generate-string (:headers payload))
        send-sql "SELECT * from pgmq.send(?,?::jsonb,?::jsonb,?::integer);"
        result (adapter/execute-one! adapter send-sql [queue-name json-payload json-headers delay])]
    (:send result)))

(defn read-message
  "Read a `quantity` of messages from `queue-name` marking them invisible for
  `visible_time` seconds using a given `adapter`. This function supports the
  ability to `filter` messages received when making a read request.

  Here are some examples of how this conditional works:
  - If conditional is an empty JSON object ('{}'::jsonb), the condition always evaluates to TRUE, and all messages are considered matching.


  - If conditional is a JSON object with a single key-value pair, such as {'key': 'value'}, the condition checks if the message column contains a JSON object with the same key-value pair. For example:
  ```
  message = {'key': 'value', 'other_key': 'other_value'}: // matches
  message = {'other_key': 'other_value'}: // does not match
  ```

  - If conditional is a JSON object with multiple key-value pairs, such as {'key1': 'value1', 'key2': 'value2'}, the condition checks if the message column contains a JSON object with all the specified key-value pairs. For example:
  ```
  message = {'key1': 'value1', 'key2': 'value2', 'other_key': 'other_value'}: // matches
  message = {'key1': 'value1', 'other_key': 'other_value'}: // does not match
  ```

  Some examples of conditional JSONB values and their effects on the query:
  * `{}`: matches all messages
  * `{'type': 'error'}`: matches messages with a type key equal to 'error'
  * `{'type': 'error', 'severity': 'high'}`: matches messages with both type equal to 'error' and severity equal to 'high'
  * `{'user_id': 123}`: matches messages with a user_id key equal to 123

  Example:
  (core/read-message adapter \"test-queue\" 10 88 nil)
  ;; => ({:msg-id 2,
          :read-ct 1,
          :enqueued-at #object[java.time.Instant 0x5f794b3d \"2025-03-21T01:14:00.831673Z\"],
          :vt #object[java.time.Instant 0x3fcde164 \"2025-03-21T01:15:32.988540Z\"],
          :message {:user-id \"0f83fbeb-345b-41ca-bbec-3bace0cff5b4\", :order-count 12},
          :headers {:TENANT \"b5bda77b-8283-4a6d-8de8-40a5041a60ee\"}})"
  [adapter queue-name visible_time quantity filter]
  (let [read-sql "SELECT * FROM pgmq.read(?,?::integer,?::integer,?::jsonb);"
        result (adapter/query adapter read-sql [queue-name visible_time quantity filter])]
    (seq result)))

(defn delete-message
  "Permanently deletes message with id `msg-id` in the queue
  named `queue-name` using a given `adapter`.

  Example:
   (core/delete-message adapter \"test-queue\" 3)
   ;; => true"
  [adapter queue-name msg-id]
  (let [delete-sql "SELECT pgmq.delete(?,?);"
        result (adapter/execute-one! adapter delete-sql [queue-name msg-id])]
    (:delete result)))

(defn pop-message
  "Pops one message from the queue named `queue-name` using a given `adapter`. The side-effect of
  this function is equivalent to reading and deleting a message. See also
  [[read-message]] and [[delete-message]].

  Example:
  (core/pop-message adapter \"test-queue\")
  ;; => {:msg-id 1,
         :read-ct 0,
         :enqueued-at #object[java.time.Instant 0x79684534 \"2025-03-20T01:29:15.298975Z\"],
         :vt #object[java.time.Instant 0x391acb50 \"2025-03-20T01:30:45.300696Z\"],
         :message {:user-id \"0f83fbeb-345b-41ca-bbec-3bace0cff5b4\", :order-count 12},
         :headers {:TENANT \"b5bda77b-8283-4a6d-8de8-40a5041a60ee\"}"
  [adapter queue-name]
  (let [pop-sql "SELECT * FROM pgmq.pop(?);"
        result (adapter/query adapter pop-sql [queue-name])]
    (first result)))

(defn archive-messages
  "Archives messages `msg-ids` in a queue named `queue-name` using a given `adapter`.
  This will remove the message from `queue-name` and place it in a archive table
  which is named `a_{queue-name}`.

  Example:
  (core/archive-messages adapter \"test-queue\" [3])
  ;; => ()"
  [adapter queue-name msg-ids]
  (let [archive-sql "SELECT * FROM pgmq.archive(?,?::bigint[]);"
        result (adapter/query adapter archive-sql [queue-name (into-array Long msg-ids)])]
    (map :archive result)))

(defn- ->jsonb-str [obj]
  (let [json-str (if (sequential? obj)
                   (ches/generate-string (mapv #(ches/generate-string %) obj))
                   (ches/generate-string obj))]
    (str/replace-first (str/replace json-str "]" "}") "[" "{")))

(defn send-message-batch
  "Sends `payload` to the queue named `queue-name` as a collection of messages
  that cannot be read for `delay` seconds using a given `adapter`. The payload
  should be a sequence of valid JSON objects. See also [[send-message]].

  Example Payloads:
   - `[{:data {:foo \"bar\"} :headers {:x-data \"bat\"}}]`
   - `[{:data 10002 :headers {}} {:data \"feed\" :headers {:version \"2\"}} ]`
  Example:
  (core/send-message-batch adapter
                               \"test-queue\"
                               [{:data {:order-count 12 :user-id \"0f83fbeb-345b-41ca-bbec-3bace0cff5b4\"} :headers {:X-SESS-ID \"b5bda77b-8283-4a6d-8de8-40a5041a60ee\"}}
                                {:data {:order-count 12 :user-id \"da04bf11-018f-45c4-908f-62c33b6e8aa6\"} :headers {:X-SESS-ID \"b0ef0d6a-e587-4c28-b995-1efe8cb31c9e\"}}]
                               15)
  ;; => [5 6]"
  [adapter queue-name payload delay]
  (let [json-payload (->jsonb-str (map :data payload))
        json-headers (->jsonb-str (map :headers payload))
        send-sql "SELECT pgmq.send_batch(?,?::jsonb[],?::jsonb[],?::integer)"
        result (adapter/execute! adapter send-sql [queue-name json-payload json-headers delay])]
    (into [] (map :send-batch) result)))

(defn delete-message-batch
  "Deletes all `msg-ids` messages in queue `queue-name` using a given `adapter`.

  Example:
  (core/delete-message-batch adapter \"test-queue\" [2 5 6])
  ;; => [2 5 6]"
  [adapter queue-name msg-ids]
  (let [delete-sql "SELECT pgmq.delete(?,?::bigint[]);"
        result (adapter/execute! adapter delete-sql [queue-name (into-array Long msg-ids)])]
    (into [] (map :delete) result)))

(if inst/instrumentation-enabled?
  (inst/enable-instrumentation)
  (inst/disable-instrumentation))
