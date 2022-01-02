(ns version-tracker.model.user
  (:require [buddy.hashers :as hashers]
            [schema.core :as s]
            [version-tracker.storage :as storage]))

(s/defn ^:private password-hash :- s/Str
  [password :- s/Str]
  (hashers/derive password {:alg :argon2id}))

(s/defn create-user! :- (s/enum ::created ::user-exists)
  [storage :- (s/protocol storage/Storage)
   username :- s/Str
   password :- s/Str]
  (let [pw-hash (password-hash password)]
    (if (storage/user-exists? storage username)
      ::user-exists
      (do
        (storage/create-user storage username pw-hash)
        ::created))))
