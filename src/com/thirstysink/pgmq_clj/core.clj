(ns com.thirstysink.pgmq-clj.core
  (:require
   [clojure.string :as str]
   [com.thirstysink.pgmq-clj.db.adapter :as adapter]))

(defn- validate-queue-name [queue-name]
  (when (or (not (string? queue-name)) (str/blank? queue-name))
    (throw (ex-info "Queue name must be a non-empty string" {:value queue-name}))))

(defn create-queue [adapter queue-name]
  (let [create-sql "SELECT pgmq.create(?)"]
    (validate-queue-name queue-name)
    (adapter/execute! adapter create-sql [queue-name])))

(defn drop-queue [adapter queue-name]
  (let [drop-sql "SELECT pgmq.drop_queue(?)"]
    (validate-queue-name queue-name)
    (adapter/execute! adapter drop-sql [queue-name])))

(defn send-message [adapter queue-name payload] nil)

(defn read-message [adapter queue-name] nil)

(defn pop-message [adapter queue-name] nil)

(defn delete-message [adapter queue-name msg-id] nil)

(defn archive-message [adapter queue-name msg-id] nil)
