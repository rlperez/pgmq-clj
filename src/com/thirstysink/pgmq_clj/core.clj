(ns com.thirstysink.pgmq-clj.core
  (:require
   [clojure.string :as str]
   [com.thirstysink.pgmq-clj.db.adapter :as adapter]))

(defn create-queue [adapter queue-name]
  (if (and (string? queue-name) (not (str/blank? queue-name)))
    (let [create-sql "SELECT pgmq.create(?)"]
      (adapter/execute! adapter create-sql [queue-name]))
    (throw (ex-info "Queue name must be a non-empty string" {:value queue-name}))))

(defn drop-queue [adapter queue-name]
  (if (and (string? queue-name) (not (str/blank? queue-name)))
    (let [create-sql "SELECT pgmq.drop_queue(?)"]
      (adapter/execute! adapter create-sql [queue-name]))
    (throw (ex-info "Queue name must be a non-empty string" {:value queue-name}))))

(defn send-message [adapter queue-name payload] nil)

(defn read-message [adapter queue-name] nil)

(defn pop-message [adapter queue-name] nil)

(defn delete-message [adapter queue-name msg-id] nil)

(defn archive-message [adapter queue-name msg-id] nil)
