(ns com.thirstysink.pgmq-clj.core)

(defn create-queue [adapter queue-name] nil)

(defn drop-queue [adapter queue-name] nil)

(defn send-message [adapter queue-name payload] nil)

(defn read-message [adapter queue-name] nil)

(defn pop-message [adapter queue-name] nil)

(defn delete-message [adapter queue-name msg-id] nil)

(defn archive-message [adapter queue-name msg-id] nil)
