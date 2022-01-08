(ns version-tracker.crypto
  (:require [buddy.core.bytes :as bytes]
            [buddy.core.codecs :as codecs]
            [buddy.core.crypto :as crypto]
            [buddy.core.nonce :as nonce]
            [schema.core :as s]
            [version-tracker.config :as config]))

(defprotocol Encrypter
  (-encrypt [this data]))

(defprotocol Decrypter
  (-decrypt [this encrypted-data]))

(s/def Bytes (s/pred bytes?))

(s/defn encrypt :- Bytes
  [crypto :- (s/protocol Encrypter) data :- s/Str]
  (-encrypt crypto data))

(s/defn decrypt :- s/Str
  [crypto :- (s/protocol Decrypter)
   encrypted-data :- Bytes]
  (-decrypt crypto encrypted-data))

(def ^:private iv-length 12)

(s/defrecord BlockCipherEncrypter [k :- Bytes]
  Encrypter
  (-encrypt [_this data]
    (let [initialization-vector (nonce/random-nonce iv-length)]
      (bytes/concat initialization-vector
                    (crypto/encrypt (codecs/str->bytes data)
                                    k
                                    initialization-vector
                                    {:algorithm :aes128-gcm})))))

(s/defrecord BlockCipherDecrypter [k :- Bytes]
  Decrypter
  (-decrypt [_this encrypted]
    (let [initialization-vector (bytes/slice encrypted 0 iv-length)
          to-decrypt (bytes/slice encrypted iv-length (count encrypted))]
      (String. ^bytes (crypto/decrypt to-decrypt
                                      k
                                      initialization-vector
                                      {:algorithm :aes128-gcm})))))

(s/defn new-block-cipher :- {:encrypter (s/protocol Encrypter)
                             :decrypter (s/protocol Decrypter)}
  [config :- config/Crypto]
  {:encrypter (->BlockCipherEncrypter (codecs/hex->bytes (:key config)))
   :decrypter (->BlockCipherDecrypter (codecs/hex->bytes (:key config)))})
