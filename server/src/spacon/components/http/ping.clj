(ns spacon.components.http.ping
  (:require [spacon.components.http.response :as response]
            [spacon.components.http.intercept :as intercept]
            [clojure.core.async :as async]
            [clojure.data.json :as json]
            [spacon.components.kafka.core :as kafka]))

(defn- pong
  "Responds with pong as a way to ensure http service is reachable"
  [_]
  (response/ok "pong"))

(defn pong-kafka
  "Subscribes to test topic and awaits pong message to ensure kafka cluster is reachable"
  [kafka-comp request]
  (let [record {:topic "test"
                :key   "anykey"
                :value (json/write-str (:json-params request))}
        promise-chan (kafka/publish kafka-comp record)
        [val _] (async/alts!! [promise-chan (async/timeout 2000)])]
    (if (nil? val)
      (response/error "Error writing to Kafka or timeout")
      (response/ok val))))

(defn routes
  [kafka-comp]
  #{["/api/ping/kafka" :post (conj intercept/common-interceptors (partial pong-kafka kafka-comp)) :route-name :pong-kafka]})
