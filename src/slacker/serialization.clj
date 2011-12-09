(ns slacker.serialization
  (:use [slacker common])
  (:require [carbonite.api :as carb])
  (:require [slacker.serialization.fastjson :as json])
  (:import [java.nio ByteBuffer])
  (:import [java.nio.charset Charset]))

(def carb-registry (atom (carb/default-registry)))

(defn read-carb
  "Deserialize clojure data structure from ByteBuffer with
  carbonite."
  [data]
  (carb/read-buffer @carb-registry data))

(defn write-carb
  "Serialize clojure data structure to ByteBuffer by carbonite"
  [data]
  (ByteBuffer/wrap (carb/write-buffer @carb-registry data)))

(defn register-serializers
  "Register additional serializers to carbonite. This allows
  slacker to transport custom data types. Caution: you should
  register serializers on both server side and client side."
  [serializers]
  (swap! carb-registry carb/register-serializers serializers))

(defn read-json
  "Deserialize clojure data structure from bytebuffer with
  jackson"
  [data]
  (let [jsonstr (.toString (.decode (Charset/forName "UTF-8") data))]
    (if *debug* (println (str "dbg:: " jsonstr)))
    (json/parse-string jsonstr)))

(defn write-json
  "Serialize clojure data structure to ByteBuffer with jackson"
  [data]
  (let [jsonstr (json/generate-string data true)]
    (if *debug* (println (str "dbg:: " jsonstr)))
    (.encode (Charset/forName "UTF-8") jsonstr)))

(defn deserializer
  "Find certain deserializer by content-type code"
  [type]
  (case type
    :json read-json
    :carb read-carb))

(defn serializer
  "Find certain serializer by content-type code"
  [type]
  (case type
    :json write-json
    :carb write-carb))

