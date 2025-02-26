(ns com.thirstysink.pgmq-clj.core
  (:require [clojure.string :as str]
            [cheshire.core :as ches]
            [com.thirstysink.pgmq-clj.db.adapter :as adapter]
            [com.thirstysink.pgmq-clj.instrumentation :as inst]))

(set! *warn-on-reflection* true)

(defn create-queue
  "Create a queue named `queue-name`."
  [adapter queue-name]
  (let [create-sql "SELECT pgmq.create(?);"]
    (adapter/execute-one! adapter create-sql [queue-name])))

(defn drop-queue
  "Drop queue named `queue-name`."
  [adapter queue-name]
  (let [drop-sql "SELECT pgmq.drop_queue(?);"
        result (adapter/execute-one! adapter drop-sql [queue-name])]
    (:drop-queue result)))

(defn list-queues
  "List all queues."
  [adapter]
  (let [list-queues-sql "SELECT * FROM pgmq.list_queues();"
        result (adapter/query adapter list-queues-sql [])]
    result))

(defn send-message
  "Send one message to a queue `queue-name` with a `payload`
  that will not be read for `delay` seconds. A `delay` of 0
  indicates it may be read immediately.

  Example Payloads:
  - `[{:data {:foo \"bad\"} :headers {:x-data \"baz\"}}]`
  - `[{:data 10022 :headers {}} {:data \"feed\" :headers {:version \"3\"}}]`"
  [adapter queue-name payload delay]
  (let [json-payload (ches/generate-string (:data payload))
        json-headers (ches/generate-string (:headers payload))
        send-sql "SELECT * from pgmq.send(?,?::jsonb,?::jsonb,?::integer);"
        result (adapter/execute-one! adapter send-sql [queue-name json-payload json-headers delay])]
    (:send result)))

(defn read-message
  "Read a `quantity` of messages from `queue-name` marking them invisible for
  `visible_time` seconds. This function supports the ability to `filter` messages
  received when making a read request.

  Here are some examples of how this conditional works:
  If conditional is an empty JSON object ('{}'::jsonb), the condition always evaluates to TRUE, and all messages are considered matching.


  If conditional is a JSON object with a single key-value pair, such as {'key': 'value'}, the condition checks if the message column contains a JSON object with the same key-value pair. For example:
  ```
  message = {'key': 'value', 'other_key': 'other_value'}: // matches
  message = {'other_key': 'other_value'}: // does not match
  ```

  If conditional is a JSON object with multiple key-value pairs, such as {'key1': 'value1', 'key2': 'value2'}, the condition checks if the message column contains a JSON object with all the specified key-value pairs. For example:
  ```
  message = {'key1': 'value1', 'key2': 'value2', 'other_key': 'other_value'}: // matches
  message = {'key1': 'value1', 'other_key': 'other_value'}: // does not match
  ```

  Some examples of conditional JSONB values and their effects on the query:
  * `{}`: matches all messages
  * `{'type': 'error'}`: matches messages with a type key equal to 'error'
  * `{'type': 'error', 'severity': 'high'}`: matches messages with both type equal to 'error' and severity equal to 'high'
  * `{'user_id': 123}`: matches messages with a user_id key equal to 123"
  [adapter queue-name visible_time quantity filter]
  (let [read-sql "SELECT * FROM pgmq.read(?,?::integer,?::integer,?::jsonb);"
        result (adapter/query adapter read-sql [queue-name visible_time quantity filter])]
    (seq result)))

(defn delete-message
  "Permanently deletes message with id `msg-id` in the queue named `queue-name`."
  [adapter queue-name msg-id]
  (let [delete-sql "SELECT pgmq.delete(?,?);"
        result (adapter/execute-one! adapter delete-sql [queue-name msg-id])]
    (:delete result)))

(defn pop-message
  "Pops one message from the queue named `queue-name`. The side-effect of
  this function is equivalent to reading and deleting a message. See also
  [[read-message]] and [[delete-message]]."
  [adapter queue-name]
  (let [pop-sql "SELECT * FROM pgmq.pop(?);"
        result (adapter/query adapter pop-sql [queue-name])]
    (first result)))

(defn archive-messages
  "Archives messages `msg-ids` in a queue named `queue-name`. This will remove the message from
  `queue-name` and place it in a archive table which is named `a_{queue-name}`."
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
  that cannot be read for `delay` seconds. The payload should be a sequence
  of valid JSON objects. See also [[send-message]].

  Example Payloads:
   - `[{:data {:foo \"bar\"} :headers {:x-data \"bat\"}}]`
   - `[{:data 10002 :headers {}} {:data \"feed\" :headers {:version \"2\"}} ]`"
  [adapter queue-name payload delay]
  (let [json-payload (->jsonb-str (map :data payload))
        json-headers (->jsonb-str (map :headers payload))
        send-sql "SELECT pgmq.send_batch(?,?::jsonb[],?::jsonb[],?::integer)"
        result (adapter/execute! adapter send-sql [queue-name json-payload json-headers delay])]
    (into [] (map :send-batch) result)))

(defn delete-message-batch
  "Deletes all `msg-ids` messages in queue `queue-name`."
  [adapter queue-name msg-ids]
  (let [delete-sql "SELECT pgmq.delete(?,?::bigint[]);"
        result (adapter/execute! adapter delete-sql [queue-name (into-array Long msg-ids)])]
    (into [] (map :delete) result)))

(if inst/instrumentation-enabled?
  (inst/enable-instrumentation)
  (inst/disable-instrumentation))
