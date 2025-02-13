(ns com.thirstysink.pgmq-clj.core-test
  (:require
   [clojure.core :as c]
   [clojure.spec.alpha :as s]
   [clojure.test :refer [deftest is testing use-fixtures]]
   [com.thirstysink.pgmq-clj.specs :as specs]
   [com.thirstysink.pgmq-clj.core :as core]
   [com.thirstysink.pgmq-clj.instrumentation :as inst]
   [com.thirstysink.util.db :as db]
   [com.thirstysink.pgmq-clj.db.adapter :as adapter]))

(defonce container (db/pgmq-container))

(use-fixtures :once
  (fn [tests]
    (try
      (inst/enable-instrumentation)
      (db/start-postgres-container container)
      (tests)
      (finally
        (inst/disable-instrumentation)
        (db/stop-postgres-container container)))))

(defn- count-entries-with-msg-id [coll msg-id]
  (count (filter #(= (:msg-id %) msg-id) coll)))

(deftest integration-tests
  (let [adapter (db/setup-adapter container)
        queue-name "test_queue"]
    (testing "create-queue and drop-queue should add and remove queues"
      (let [queue-name-1 "test_queue_1"
            queue-name-2 "test_queue_2"]
        (let [queues (core/list-queues adapter)]
          (is (= 0 (count queues))))
        (core/create-queue adapter queue-name-1)
        (let [queues (core/list-queues adapter)]
          (is (= 1 (count queues)))
          (is (= queue-name-1 (:queue-name (first queues)))))
        (core/create-queue adapter queue-name-2)
        (let [queues (core/list-queues adapter)]
          (is (= 2 (count queues)))
          (is (some #(= queue-name-2 (:queue-name %)) queues)))
        (let [drop-queue-1-result (core/drop-queue adapter queue-name-1)
              queues (core/list-queues adapter)]
          (is (= drop-queue-1-result true))
          (is (= 1 (count queues)))
          (is (some #(= queue-name-2 (:queue-name %)) queues))
          (is (not (some #(= queue-name-1 (:queue-name %)) queues)))
          (is (= 1 (count queues))))
        (let [drop-queue-result (core/drop-queue adapter queue-name-2)
              queues (core/list-queues adapter)]
          (is (= drop-queue-result true))
          (is (= 0 (count queues))))))
    (testing "send-message should send and return an id"
      (core/create-queue adapter queue-name)
      (let [payload {:foo "bar"}
            headers {:x-my-data "yup"}
            result (core/send-message adapter queue-name {:data payload :headers headers} 0)]
        (is (some? result))
        (is (number? result))
        (is (= result 1)))
      (core/drop-queue adapter queue-name))
    (testing "read-message should respect visibility time"
      (core/create-queue adapter queue-name)
      (let [visibility-time 1
            quantity 2]
        ;; Send two messages to a fresh queue
        (let [payload {:foo "bar"}
              headers {:x-my-data "yup"}]
          (core/send-message adapter queue-name {:data payload :headers headers} 0))
        (let [payload {:foo "baz"}
              headers {:x-my-data "no"}]
          (core/send-message adapter queue-name {:data payload :headers headers} 0))
        ;; Filtering on a message with a foo set to bar we should only get one.
        (let [result-filtered (core/read-message adapter queue-name visibility-time quantity {:foo "bar"})
              first-result (first result-filtered)]
          (is (seq result-filtered))
          (is (= 1 (count result-filtered)))
          (is (= {:x-my-data "yup"} (get-in first-result [:headers])))
          (is (= 1 (get-in first-result [:msg-id]))))
        ;; Reading for foo bar again should be empty due to visibility rules
        (let [result-bar-before (core/read-message adapter queue-name visibility-time quantity {:foo "bar"})]
          (is (empty? result-bar-before)))
        ;; Reading unfiltered now should also fetch foo baz
        (let [result-baz-before (core/read-message adapter queue-name visibility-time quantity {})]
          (is (seq result-baz-before))
          (is (= 1 (count result-baz-before)))
          (is (= (get-in (first result-baz-before) [:msg-id]) 2)))
        (Thread/sleep 1500)
        ;; After sleeping past the visibility time we should have both foos, bar and baz
        (let [result-after (core/read-message adapter queue-name visibility-time quantity {})]
          (is (= 2 (count result-after)))))
      (core/drop-queue adapter queue-name))
    (testing "delete-message should delete messages"
      (core/create-queue adapter queue-name)
      (let [msg-id (core/send-message adapter queue-name {:data {:foo "bar"} :headers {:baz "bat"}} 1)]
        (is (true? (core/delete-message adapter queue-name msg-id)))
        (is (nil? (core/read-message adapter queue-name 1 1 {}))))
      (core/drop-queue adapter queue-name))
    (testing "delete message that doesn't exist"
      (core/create-queue adapter queue-name)
      (is (false? (core/delete-message adapter queue-name 18728)))
      (core/drop-queue adapter queue-name))
    (testing "pop-message should return one message and remove it from queue"
      (core/create-queue adapter queue-name)
      (let [message {:foo "bar"}
            _ (core/send-message adapter queue-name {:data message :headers {}} 0)
            popped-message (core/pop-message adapter queue-name)]
        (is (s/valid? ::specs/message-record popped-message))
        (is (nil? (core/pop-message adapter queue-name)))))
    (testing "archive-message should move 1 message to an archive table"
      (core/create-queue adapter queue-name)
      (let [message {:foo "bar"}
            msg-id-1 (core/send-message adapter queue-name {:data message :headers {}} 0)
            _ (core/read-message adapter queue-name 30 30 {})
            archive-ids (core/archive-message adapter queue-name [msg-id-1])
            archive (adapter/query adapter (format "SELECT * FROM pgmq.a_%s;" queue-name) [])]
        (is (s/valid? ::specs/msg-ids archive-ids))
        (is (= 1 (count-entries-with-msg-id archive msg-id-1)))
        (is (= (map :msg-id archive) archive-ids))
        (core/drop-queue adapter queue-name)))
    (testing "archive-message should move 2 messages to an archive table"
      (core/create-queue adapter queue-name)
      (let [message {:foo "bar"}
            msg-id-1 (core/send-message adapter queue-name {:data message :headers {}} 0)
            msg-id-2  (core/send-message adapter queue-name {:data message :headers {}} 0)
            _ (core/read-message adapter queue-name 30 30 {})
            archive-ids (core/archive-message adapter queue-name [msg-id-1 msg-id-2])
            archive (adapter/query adapter (format "SELECT * FROM pgmq.a_%s;" queue-name) [])]
        (is (s/valid? ::specs/msg-ids archive-ids))
        (is (= 1 (count-entries-with-msg-id archive msg-id-1)))
        (is (= 1 (count-entries-with-msg-id archive msg-id-2)))
        (is (= (map :msg-id archive) archive-ids))
        (core/drop-queue adapter queue-name)))
    (testing "send-message-batch should send and return a list of ids"
      (core/create-queue adapter queue-name)
      (let [payload [{:data "bar" :headers {:x-my-data "yup"}}
                     {:data {:baz "bat"} :headers {:x-my-data "nope"}}]
            result (core/send-message-batch adapter queue-name payload 0)]
        (is (some? result))
        (is (coll? result))
        (is (= result [1 2])))
      (core/drop-queue adapter queue-name))
    (testing "delete-message-batch should delete multiple messages"
      (core/create-queue adapter queue-name)
      (let [payload [{:data "bar" :headers {:x-my-data "yup"}}
                     {:data {:baz "bat"} :headers {:x-my-data "nope"}}]
            send-result (core/send-message-batch adapter queue-name payload 0)
            read-result (core/read-message adapter queue-name 0 30 {})
            delete-result (core/delete-message-batch adapter queue-name [1 2 3])]
        (is (= send-result [1 2]))
        (is (= (count read-result) 2))
        (is (= delete-result [1 2])))
      (core/drop-queue adapter queue-name))
    (adapter/close adapter)))
