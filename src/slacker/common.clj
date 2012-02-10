(ns slacker.common)

(def
  ^{:dynamic true
    :doc "Debug flag. This flag can be override by binding if you like to see some debug output."}
  *debug* false)
(def
  ^{:dynamic true
    :doc "Timeout for synchronouse call."}
  *timeout* 10000)
(def
  ^{:dynamic true
    :doc "Protocol version."}
  version (short 4))
