package com.cloud.framework.security.keys;

import com.cloud.framework.config.ConfigKey;

/**
 * Started this file to manage keys.  Will be needed by other services.
 */
public interface KeysManager {
    final ConfigKey<String> EncryptionKey = new ConfigKey<>("Hidden", String.class, "security.encryption.key", null, "base64 encoded key data", false);
    final ConfigKey<String> EncryptionIV = new ConfigKey<>("Hidden", String.class, "security.encryption.iv", null, "base64 encoded IV data", false);
    final ConfigKey<String> HashKey = new ConfigKey<>("Hidden", String.class, "security.hash.key", null, "for generic key-ed hash", false);
    final ConfigKey<String> AuthenticationKey = new ConfigKey<>("Hidden", String.class, "security.authentication.key", null, "base64 encoded authentication data", false);

    String getEncryptionKey();

    String getEncryptionIV();

    void resetEncryptionKeyIV();

    String getHashKey();

    String getAuthenticationKey();
}
