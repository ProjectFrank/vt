(ns version-tracker.crypto-test
  (:require [buddy.core.codecs :as codecs]
            [buddy.core.nonce :as nonce]
            [clojure.test :refer [deftest is use-fixtures]]
            [version-tracker.crypto :as crypto]
            [version-tracker.test-utils :as test-utils]))

(use-fixtures :once test-utils/schema-validation-fixture)

(deftest crypto-test
  (let [value "some value"
        key (codecs/bytes->hex (nonce/random-nonce 16))
        {:keys [encrypter decrypter]} (crypto/new-block-cipher {:key key})]
    (is (= value
           (->> value
                (crypto/encrypt encrypter)
                (crypto/decrypt decrypter))))))
