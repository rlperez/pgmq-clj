[![codecov](https://codecov.io/github/rlperez/pgmq-clj/graph/badge.svg?token=KIC7UP13WY)](https://codecov.io/github/rlperez/pgmq-clj)


# pgmq-clj
A PGMQ library written in Clojure


# Documentation
# Table of contents
-  [`com.thirstysink.pgmq-clj.core`](#com.thirstysink.pgmq-clj.core) 
    -  [`archive-messages`](#com.thirstysink.pgmq-clj.core/archive-messages) - Archives messages <code>msg-ids</code> in a queue named <code>queue-name</code>.
    -  [`create-queue`](#com.thirstysink.pgmq-clj.core/create-queue) - Create a queue named <code>queue-name</code>.
    -  [`delete-message`](#com.thirstysink.pgmq-clj.core/delete-message) - Permanently deletes message with id <code>msg-id</code> in the queue named <code>queue-name</code>.
    -  [`delete-message-batch`](#com.thirstysink.pgmq-clj.core/delete-message-batch) - Deletes all <code>msg-ids</code> messages in queue <code>queue-name</code>.
    -  [`drop-queue`](#com.thirstysink.pgmq-clj.core/drop-queue) - Drop queue named <code>queue-name</code>.
    -  [`list-queues`](#com.thirstysink.pgmq-clj.core/list-queues) - List all queues.
    -  [`pop-message`](#com.thirstysink.pgmq-clj.core/pop-message) - Pops one message from the queue named <code>queue-name</code>.
    -  [`read-message`](#com.thirstysink.pgmq-clj.core/read-message) - Read a <code>quantity</code> of messages from <code>queue-name</code> marking them invisible for <code>visible_time</code> seconds.
    -  [`send-message`](#com.thirstysink.pgmq-clj.core/send-message) - Send one message to a queue <code>queue-name</code> with a <code>payload</code> that will not be read for <code>delay</code> seconds.
    -  [`send-message-batch`](#com.thirstysink.pgmq-clj.core/send-message-batch) - Sends <code>payload</code> to the queue named <code>queue-name</code> as a collection of messages that cannot be read for <code>delay</code> seconds.
-  [`com.thirstysink.pgmq-clj.db.adapter`](#com.thirstysink.pgmq-clj.db.adapter) 
    -  [`Adapter`](#com.thirstysink.pgmq-clj.db.adapter/Adapter)
    -  [`close`](#com.thirstysink.pgmq-clj.db.adapter/close) - Performs database connection cleanup.
    -  [`execute!`](#com.thirstysink.pgmq-clj.db.adapter/execute!) - Execute a SQL statement with 0 or more return values.
    -  [`execute-one!`](#com.thirstysink.pgmq-clj.db.adapter/execute-one!) - Execute a SQL statement with 0 or 1 return values.
    -  [`query`](#com.thirstysink.pgmq-clj.db.adapter/query) - Query the database and return results.
    -  [`with-transaction`](#com.thirstysink.pgmq-clj.db.adapter/with-transaction) - Wrap a function in a database transaction.
-  [`com.thirstysink.pgmq-clj.db.adapters.hikari-adapter`](#com.thirstysink.pgmq-clj.db.adapters.hikari-adapter) 
    -  [`->pgobject`](#com.thirstysink.pgmq-clj.db.adapters.hikari-adapter/->pgobject) - Transforms Clojure data to a PGobject that contains the data as JSON.
    -  [`<-pgobject`](#com.thirstysink.pgmq-clj.db.adapters.hikari-adapter/<-pgobject) - Transform PGobject containing <code>json</code> or <code>jsonb</code> value to Clojure data.
    -  [`ensure-pgmq-extension`](#com.thirstysink.pgmq-clj.db.adapters.hikari-adapter/ensure-pgmq-extension) - Checks the database to verify that the <code>pgmq</code> extension is installed.
    -  [`make-hikari-adapter`](#com.thirstysink.pgmq-clj.db.adapters.hikari-adapter/make-hikari-adapter) - Create a new HikariAdapter instance.
-  [`com.thirstysink.pgmq-clj.instrumentation`](#com.thirstysink.pgmq-clj.instrumentation) 
    -  [`disable-instrumentation`](#com.thirstysink.pgmq-clj.instrumentation/disable-instrumentation) - Disables <code>clojure.specs.alpha</code> specs instrumentation.
    -  [`enable-instrumentation`](#com.thirstysink.pgmq-clj.instrumentation/enable-instrumentation) - Enables <code>clojure.specs.alpha</code> specs instrumentation.
    -  [`instrumentation-enabled?`](#com.thirstysink.pgmq-clj.instrumentation/instrumentation-enabled?)
-  [`com.thirstysink.pgmq-clj.json`](#com.thirstysink.pgmq-clj.json) 
    -  [`->json`](#com.thirstysink.pgmq-clj.json/->json)
-  [`com.thirstysink.pgmq-clj.specs`](#com.thirstysink.pgmq-clj.specs) 

-----
# <a name="com.thirstysink.pgmq-clj.core">com.thirstysink.pgmq-clj.core</a>






## <a name="com.thirstysink.pgmq-clj.core/archive-messages">`archive-messages`</a><a name="com.thirstysink.pgmq-clj.core/archive-messages"></a>
``` clojure

(archive-messages adapter queue-name msg-ids)
```
Function.

Archives messages `msg-ids` in a queue named `queue-name`. This will remove the message from
  `queue-name` and place it in a archive table which is named `a_{queue-name}`.
<p><sub><a href="/blob/main/src/com/thirstysink/pgmq_clj/core.clj#L91-L97">Source</a></sub></p>

## <a name="com.thirstysink.pgmq-clj.core/create-queue">`create-queue`</a><a name="com.thirstysink.pgmq-clj.core/create-queue"></a>
``` clojure

(create-queue adapter queue-name)
```
Function.

Create a queue named `queue-name`.
<p><sub><a href="/blob/main/src/com/thirstysink/pgmq_clj/core.clj#L9-L13">Source</a></sub></p>

## <a name="com.thirstysink.pgmq-clj.core/delete-message">`delete-message`</a><a name="com.thirstysink.pgmq-clj.core/delete-message"></a>
``` clojure

(delete-message adapter queue-name msg-id)
```
Function.

Permanently deletes message with id `msg-id` in the queue named `queue-name`.
<p><sub><a href="/blob/main/src/com/thirstysink/pgmq_clj/core.clj#L75-L80">Source</a></sub></p>

## <a name="com.thirstysink.pgmq-clj.core/delete-message-batch">`delete-message-batch`</a><a name="com.thirstysink.pgmq-clj.core/delete-message-batch"></a>
``` clojure

(delete-message-batch adapter queue-name msg-ids)
```
Function.

Deletes all `msg-ids` messages in queue `queue-name`.
<p><sub><a href="/blob/main/src/com/thirstysink/pgmq_clj/core.clj#L120-L125">Source</a></sub></p>

## <a name="com.thirstysink.pgmq-clj.core/drop-queue">`drop-queue`</a><a name="com.thirstysink.pgmq-clj.core/drop-queue"></a>
``` clojure

(drop-queue adapter queue-name)
```
Function.

Drop queue named `queue-name`.
<p><sub><a href="/blob/main/src/com/thirstysink/pgmq_clj/core.clj#L15-L20">Source</a></sub></p>

## <a name="com.thirstysink.pgmq-clj.core/list-queues">`list-queues`</a><a name="com.thirstysink.pgmq-clj.core/list-queues"></a>
``` clojure

(list-queues adapter)
```
Function.

List all queues.
<p><sub><a href="/blob/main/src/com/thirstysink/pgmq_clj/core.clj#L22-L27">Source</a></sub></p>

## <a name="com.thirstysink.pgmq-clj.core/pop-message">`pop-message`</a><a name="com.thirstysink.pgmq-clj.core/pop-message"></a>
``` clojure

(pop-message adapter queue-name)
```
Function.

Pops one message from the queue named `queue-name`. The side-effect of
  this function is equivalent to reading and deleting a message. See also
  [[read-message]] and [[delete-message]].
<p><sub><a href="/blob/main/src/com/thirstysink/pgmq_clj/core.clj#L82-L89">Source</a></sub></p>

## <a name="com.thirstysink.pgmq-clj.core/read-message">`read-message`</a><a name="com.thirstysink.pgmq-clj.core/read-message"></a>
``` clojure

(read-message adapter queue-name visible_time quantity filter)
```
Function.

Read a `quantity` of messages from `queue-name` marking them invisible for
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
  * `{'user_id': 123}`: matches messages with a user_id key equal to 123
<p><sub><a href="/blob/main/src/com/thirstysink/pgmq_clj/core.clj#L44-L73">Source</a></sub></p>

## <a name="com.thirstysink.pgmq-clj.core/send-message">`send-message`</a><a name="com.thirstysink.pgmq-clj.core/send-message"></a>
``` clojure

(send-message adapter queue-name payload delay)
```
Function.

Send one message to a queue `queue-name` with a `payload`
  that will not be read for `delay` seconds. A `delay` of 0
  indicates it may be read immediately.

  Example Payloads:
  - `[{:data {:foo "bad"} :headers {:x-data "baz"}}]`
  - `[{:data 10022 :headers {}} {:data "feed" :headers {:version "3"}}]`
<p><sub><a href="/blob/main/src/com/thirstysink/pgmq_clj/core.clj#L29-L42">Source</a></sub></p>

## <a name="com.thirstysink.pgmq-clj.core/send-message-batch">`send-message-batch`</a><a name="com.thirstysink.pgmq-clj.core/send-message-batch"></a>
``` clojure

(send-message-batch adapter queue-name payload delay)
```
Function.

Sends `payload` to the queue named `queue-name` as a collection of messages
  that cannot be read for `delay` seconds. The payload should be a sequence
  of valid JSON objects. See also [[send-message]].

  Example Payloads:
   - `[{:data {:foo "bar"} :headers {:x-data "bat"}}]`
   - `[{:data 10002 :headers {}} {:data "feed" :headers {:version "2"}} ]`
<p><sub><a href="/blob/main/src/com/thirstysink/pgmq_clj/core.clj#L105-L118">Source</a></sub></p>

-----
# <a name="com.thirstysink.pgmq-clj.db.adapter">com.thirstysink.pgmq-clj.db.adapter</a>






## <a name="com.thirstysink.pgmq-clj.db.adapter/Adapter">`Adapter`</a><a name="com.thirstysink.pgmq-clj.db.adapter/Adapter"></a>



<p><sub><a href="/blob/main/src/com/thirstysink/pgmq_clj/db/adapter.clj#L3-L8">Source</a></sub></p>

## <a name="com.thirstysink.pgmq-clj.db.adapter/close">`close`</a><a name="com.thirstysink.pgmq-clj.db.adapter/close"></a>
``` clojure

(close this)
```
Function.

Performs database connection cleanup.
<p><sub><a href="/blob/main/src/com/thirstysink/pgmq_clj/db/adapter.clj#L8-L8">Source</a></sub></p>

## <a name="com.thirstysink.pgmq-clj.db.adapter/execute!">`execute!`</a><a name="com.thirstysink.pgmq-clj.db.adapter/execute!"></a>
``` clojure

(execute! this sql params)
```
Function.

Execute a SQL statement with 0 or more return values.
<p><sub><a href="/blob/main/src/com/thirstysink/pgmq_clj/db/adapter.clj#L5-L5">Source</a></sub></p>

## <a name="com.thirstysink.pgmq-clj.db.adapter/execute-one!">`execute-one!`</a><a name="com.thirstysink.pgmq-clj.db.adapter/execute-one!"></a>
``` clojure

(execute-one! this sql params)
```
Function.

Execute a SQL statement with 0 or 1 return values.
<p><sub><a href="/blob/main/src/com/thirstysink/pgmq_clj/db/adapter.clj#L4-L4">Source</a></sub></p>

## <a name="com.thirstysink.pgmq-clj.db.adapter/query">`query`</a><a name="com.thirstysink.pgmq-clj.db.adapter/query"></a>
``` clojure

(query this sql params)
```
Function.

Query the database and return results.
<p><sub><a href="/blob/main/src/com/thirstysink/pgmq_clj/db/adapter.clj#L6-L6">Source</a></sub></p>

## <a name="com.thirstysink.pgmq-clj.db.adapter/with-transaction">`with-transaction`</a><a name="com.thirstysink.pgmq-clj.db.adapter/with-transaction"></a>
``` clojure

(with-transaction this f)
```
Function.

Wrap a function in a database transaction.
<p><sub><a href="/blob/main/src/com/thirstysink/pgmq_clj/db/adapter.clj#L7-L7">Source</a></sub></p>

-----
# <a name="com.thirstysink.pgmq-clj.db.adapters.hikari-adapter">com.thirstysink.pgmq-clj.db.adapters.hikari-adapter</a>






## <a name="com.thirstysink.pgmq-clj.db.adapters.hikari-adapter/->pgobject">`->pgobject`</a><a name="com.thirstysink.pgmq-clj.db.adapters.hikari-adapter/->pgobject"></a>
``` clojure

(->pgobject x)
```
Function.

Transforms Clojure data to a PGobject that contains the data as
  JSON. PGObject type defaults to `jsonb` but can be changed via
  metadata key `:pgtype`
<p><sub><a href="/blob/main/src/com/thirstysink/pgmq_clj/db/adapters/hikari_adapter.clj#L79-L87">Source</a></sub></p>

## <a name="com.thirstysink.pgmq-clj.db.adapters.hikari-adapter/<-pgobject">`<-pgobject`</a><a name="com.thirstysink.pgmq-clj.db.adapters.hikari-adapter/<-pgobject"></a>
``` clojure

(<-pgobject v)
```
Function.

Transform PGobject containing `json` or `jsonb` value to Clojure data.
<p><sub><a href="/blob/main/src/com/thirstysink/pgmq_clj/db/adapters/hikari_adapter.clj#L89-L99">Source</a></sub></p>

## <a name="com.thirstysink.pgmq-clj.db.adapters.hikari-adapter/ensure-pgmq-extension">`ensure-pgmq-extension`</a><a name="com.thirstysink.pgmq-clj.db.adapters.hikari-adapter/ensure-pgmq-extension"></a>
``` clojure

(ensure-pgmq-extension adapter)
```
Function.

Checks the database to verify that the `pgmq` extension is installed.
  If it is not then it will throw an exception.
<p><sub><a href="/blob/main/src/com/thirstysink/pgmq_clj/db/adapters/hikari_adapter.clj#L121-L128">Source</a></sub></p>

## <a name="com.thirstysink.pgmq-clj.db.adapters.hikari-adapter/make-hikari-adapter">`make-hikari-adapter`</a><a name="com.thirstysink.pgmq-clj.db.adapters.hikari-adapter/make-hikari-adapter"></a>
``` clojure

(make-hikari-adapter config)
```
Function.

Create a new HikariAdapter instance. The argument config
  provides database connection values. See https://github.com/tomekw/hikari-cp
  for additional details on the configuration options.

  | Setting         | Description                                                                                                  |
  | :-------------- | :----------------------------------------------------------------------------------------------------------- |
  | JdbcUrl         | This property sets the JDBC connection URL.                                                                            |
  | Username        | This property sets the default authentication username used when obtaining Connections from the underlying driver.     |
  | Password        | This property sets the default authentication password used when obtaining Connections from the underlying driver.     |
  | MaximumPoolSize | This property controls the maximum size that the pool is allowed to reach, including both idle and in-use connections. |
  | MinimumIdle     | This property controls the minimum number of idle connections that HikariCP tries to maintain in the pool.             |
<p><sub><a href="/blob/main/src/com/thirstysink/pgmq_clj/db/adapters/hikari_adapter.clj#L130-L151">Source</a></sub></p>

-----
# <a name="com.thirstysink.pgmq-clj.instrumentation">com.thirstysink.pgmq-clj.instrumentation</a>






## <a name="com.thirstysink.pgmq-clj.instrumentation/disable-instrumentation">`disable-instrumentation`</a><a name="com.thirstysink.pgmq-clj.instrumentation/disable-instrumentation"></a>
``` clojure

(disable-instrumentation)
(disable-instrumentation ns)
```
Function.

Disables `clojure.specs.alpha` specs instrumentation.
  [Learn more](https://github.com/clojure/spec.alpha). If
  no namespace is provided it will disable instrumentation
  for [`com.thirstysink.pgmq-clj.core`](#com.thirstysink.pgmq-clj.core).
<p><sub><a href="/blob/main/src/com/thirstysink/pgmq_clj/instrumentation.clj#L20-L28">Source</a></sub></p>

## <a name="com.thirstysink.pgmq-clj.instrumentation/enable-instrumentation">`enable-instrumentation`</a><a name="com.thirstysink.pgmq-clj.instrumentation/enable-instrumentation"></a>
``` clojure

(enable-instrumentation)
(enable-instrumentation ns)
```
Function.

Enables `clojure.specs.alpha` specs instrumentation.
  [Learn more](https://github.com/clojure/spec.alpha). If
  no namespace is provided it will instrument [`com.thirstysink.pgmq-clj.core`](#com.thirstysink.pgmq-clj.core).
<p><sub><a href="/blob/main/src/com/thirstysink/pgmq_clj/instrumentation.clj#L11-L18">Source</a></sub></p>

## <a name="com.thirstysink.pgmq-clj.instrumentation/instrumentation-enabled?">`instrumentation-enabled?`</a><a name="com.thirstysink.pgmq-clj.instrumentation/instrumentation-enabled?"></a>



<p><sub><a href="/blob/main/src/com/thirstysink/pgmq_clj/instrumentation.clj#L4-L4">Source</a></sub></p>

-----
# <a name="com.thirstysink.pgmq-clj.json">com.thirstysink.pgmq-clj.json</a>






## <a name="com.thirstysink.pgmq-clj.json/->json">`->json`</a><a name="com.thirstysink.pgmq-clj.json/->json"></a>



<p><sub><a href="/blob/main/src/com/thirstysink/pgmq_clj/json.clj#L4-L4">Source</a></sub></p>

-----


# Specs
### :com.thirstysink.pgmq-clj.specs/message-records
```clojure
(coll-of :com.thirstysink.pgmq-clj.specs/mesage-record)
```

### :com.thirstysink.pgmq-clj.specs/quantity
```clojure
(and int? (> % 0))
```

### :com.thirstysink.pgmq-clj.specs/queue-record
```clojure
(keys :req-un [:com.thirstysink.pgmq-clj.specs/queue-name :com.thirstysink.pgmq-clj.specs/is-partitioned :com.thirstysink.pgmq-clj.specs/is-unlogged :com.thirstysink.pgmq-clj.specs/created-at])
```

### com.thirstysink.pgmq-clj.core/send-message
```clojure
(fspec :args (cat :adapter :com.thirstysink.pgmq-clj.specs/adapter :queue-name :com.thirstysink.pgmq-clj.specs/queue-name :payload :com.thirstysink.pgmq-clj.specs/payload-object :delay :com.thirstysink.pgmq-clj.specs/delay) :ret :com.thirstysink.pgmq-clj.specs/msg-id :fn nil)
```

### :com.thirstysink.pgmq-clj.specs/json
```clojure
(fn [x] (or (map? x) (vector? x) (string? x) (number? x) (boolean? x) (nil? x)))
```

### :com.thirstysink.pgmq-clj.specs/enqueued-at
```clojure
(instance? java.time.Instant %)
```

### :com.thirstysink.pgmq-clj.specs/header-value
```clojure
(or :string string? :number number? :list (coll-of (or :string string? :number number?)))
```

### com.thirstysink.pgmq-clj.core/list-queues
```clojure
(fspec :args (cat :adapter :com.thirstysink.pgmq-clj.specs/adapter) :ret :com.thirstysink.pgmq-clj.specs/queue-result :fn nil)
```

### :com.thirstysink.pgmq-clj.specs/message-record
```clojure
(keys :req-un [:com.thirstysink.pgmq-clj.specs/msg-id :com.thirstysink.pgmq-clj.specs/read-ct :com.thirstysink.pgmq-clj.specs/enqueued-at :com.thirstysink.pgmq-clj.specs/vt :com.thirstysink.pgmq-clj.specs/message] :opt-un [:com.thirstysink.pgmq-clj.specs/headers])
```

### com.thirstysink.pgmq-clj.core/drop-queue
```clojure
(fspec :args (cat :adapter :com.thirstysink.pgmq-clj.specs/adapter :queue-name :com.thirstysink.pgmq-clj.specs/queue-name) :ret boolean? :fn nil)
```

### :com.thirstysink.pgmq-clj.specs/delay
```clojure
int?
```

### :com.thirstysink.pgmq-clj.specs/adapter
```clojure
(satisfies? Adapter %)
```

### :com.thirstysink.pgmq-clj.specs/payload-object
```clojure
(keys :req-un [:com.thirstysink.pgmq-clj.specs/data :com.thirstysink.pgmq-clj.specs/headers])
```

### :com.thirstysink.pgmq-clj.specs/payload-objects
```clojure
(coll-of :com.thirstysink.pgmq-clj.specs/payload-object)
```

### com.thirstysink.pgmq-clj.core/delete-message-batch
```clojure
(fspec :args (cat :adapter :com.thirstysink.pgmq-clj.specs/adapter :queue-name :com.thirstysink.pgmq-clj.specs/queue-name :msg-ids :com.thirstysink.pgmq-clj.specs/non-empty-msg-ids) :ret :com.thirstysink.pgmq-clj.specs/msg-ids :fn nil)
```

### :com.thirstysink.pgmq-clj.specs/visibility_time
```clojure
(and int? (>= % 0))
```

### com.thirstysink.pgmq-clj.core/pop-message
```clojure
(fspec :args (cat :adapter :com.thirstysink.pgmq-clj.specs/adapter :queue-name :com.thirstysink.pgmq-clj.specs/queue-name) :ret :com.thirstysink.pgmq-clj.specs/message-record :fn nil)
```

### :com.thirstysink.pgmq-clj.specs/queue-result
```clojure
(coll-of :com.thirstysink.pgmq-clj.specs/queue-record)
```

### :com.thirstysink.pgmq-clj.specs/non-empty-msg-ids
```clojure
(and :com.thirstysink.pgmq-clj.specs/msg-ids (complement empty?))
```

### com.thirstysink.pgmq-clj.core/create-queue
```clojure
(fspec :args (cat :adapter :com.thirstysink.pgmq-clj.specs/adapter :queue-name :com.thirstysink.pgmq-clj.specs/queue-name) :ret nil :fn nil)
```

### :com.thirstysink.pgmq-clj.specs/is-partitioned
```clojure
boolean?
```

### :com.thirstysink.pgmq-clj.specs/queue-name
```clojure
valid-queue-name?
```

### com.thirstysink.pgmq-clj.core/archive-message
```clojure
(fspec :args (cat :adapter :com.thirstysink.pgmq-clj.specs/adapter :queue-name :com.thirstysink.pgmq-clj.specs/queue-name :msg-ids :com.thirstysink.pgmq-clj.specs/msg-ids) :ret :com.thirstysink.pgmq-clj.specs/msg-ids :fn nil)
```

### :com.thirstysink.pgmq-clj.specs/headers
```clojure
(nilable (map-of :com.thirstysink.pgmq-clj.specs/header-key :com.thirstysink.pgmq-clj.specs/header-value :min-count 0))
```

### :com.thirstysink.pgmq-clj.specs/created-at
```clojure
(fn [x] (fn* [] (instance? java.time.Instant x)))
```

### com.thirstysink.pgmq-clj.core/send-message-batch
```clojure
(fspec :args (cat :adapter :com.thirstysink.pgmq-clj.specs/adapter :queue-name :com.thirstysink.pgmq-clj.specs/queue-name :payload :com.thirstysink.pgmq-clj.specs/payload-objects :delay :com.thirstysink.pgmq-clj.specs/delay) :ret :com.thirstysink.pgmq-clj.specs/msg-ids :fn nil)
```

### :com.thirstysink.pgmq-clj.specs/msg-ids
```clojure
(coll-of :com.thirstysink.pgmq-clj.specs/msg-id)
```

### :com.thirstysink.pgmq-clj.specs/message
```clojure
(fn [x] (or (map? x) (vector? x) (string? x) (number? x) (boolean? x) (nil? x)))
```

### :com.thirstysink.pgmq-clj.specs/data
```clojure
(fn [x] (or (map? x) (vector? x) (string? x) (number? x) (boolean? x) (nil? x)))
```

### :clojure.spec.alpha/kvs->map
```clojure
(conformer (zipmap (map :clojure.spec.alpha/k %) (map :clojure.spec.alpha/v %)) (map (fn [[k v]] #:clojure.spec.alpha{:k k, :v v}) %))
```

### com.thirstysink.pgmq-clj.core/read-message
```clojure
(fspec :args (cat :adapter :com.thirstysink.pgmq-clj.specs/adapter :queue-name :com.thirstysink.pgmq-clj.specs/queue-name :visibility_time :com.thirstysink.pgmq-clj.specs/visibility_time :quantity :com.thirstysink.pgmq-clj.specs/quantity :filter :com.thirstysink.pgmq-clj.specs/json) :ret :com.thirstysink.pgmq-clj.specs/message-records :fn nil)
```

### :com.thirstysink.pgmq-clj.specs/read-ct
```clojure
int?
```

### :com.thirstysink.pgmq-clj.specs/timestamp
```clojure
(instance? java.time.Instant %)
```

### :com.thirstysink.pgmq-clj.specs/msg-id
```clojure
(and number? pos?)
```

### :com.thirstysink.pgmq-clj.specs/vt
```clojure
(instance? java.time.Instant %)
```

### :com.thirstysink.pgmq-clj.specs/is-unlogged
```clojure
boolean?
```

### :com.thirstysink.pgmq-clj.specs/header-key
```clojure
(or :string string? :keyword keyword?)
```

### com.thirstysink.pgmq-clj.core/delete-message
```clojure
(fspec :args (cat :adapter :com.thirstysink.pgmq-clj.specs/adapter :queue-name :com.thirstysink.pgmq-clj.specs/queue-name :msg-id :com.thirstysink.pgmq-clj.specs/msg-id) :ret boolean? :fn nil)
```
