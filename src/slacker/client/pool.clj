(ns slacker.client.pool
  (:use [slacker protocol common])
  (:use [slacker.client common])
  (:use [lamina.core :exclude [close]])
  (:use [lamina connections])
  (:use [aleph tcp])
  (:import [org.apache.commons.pool PoolableObjectFactory])
  (:import [org.apache.commons.pool.impl GenericObjectPool]))

(defn- connection-pool-factory [host port]
  (reify
    PoolableObjectFactory
    (destroyObject [this obj]
      (close-connection obj))
    (makeObject [this]
      (client #(tcp-client {:host host
                            :port port
                            :frame slacker-base-codec})))
    (validateObject [this obj]
      true)
    (activateObject [this obj]
      )
    (passivateObject [this obj]
      )))

(def exhausted-actions
  {:fail GenericObjectPool/WHEN_EXHAUSTED_FAIL
   :block GenericObjectPool/WHEN_EXHAUSTED_BLOCK
   :grow GenericObjectPool/WHEN_EXHAUSTED_GROW})

(defn connection-pool
  [host port max-active exhausted-action max-wait max-idle]
  (let [factory (connection-pool-factory host port)]
    (GenericObjectPool. factory
                        max-active
                        (exhausted-actions exhausted-action)
                        max-wait
                        max-idle)))

(deftype PooledSlackerClient [pool content-type]
  SlackerClientProtocol
  (sync-call-remote [this ns-name func-name params]
    (let [conn (.borrowObject pool)
          fname (str ns-name "/" func-name)
          request (make-request content-type fname params)
          response (wait-for-result (conn request) *timeout*)]
      (.returnObject pool conn)
      (when-let [[_ resp] response]
        (handle-response resp))))
  (async-call-remote [this ns-name func-name params cb]
    (let [conn (.borrowObject pool)
          fname (str ns-name "/" func-name)
          request (make-request content-type fname params)]
      (run-pipeline
       (conn request)
       #(do
          (.returnObject pool conn)
          (if-let [[_ resp] %]
            (let [result (handle-response resp)]
              (if-not (nil? cb) (cb result))
              result))))))
  (inspect [this cmd args]
    (let [conn (.borrowObject pool)
          request (make-inspect-request cmd args)
          response (wait-for-result (conn request) *timeout*)]
      (.returnObject pool conn)
      (parse-inspect-response response)))
  (close [this]
    (.close pool)))


