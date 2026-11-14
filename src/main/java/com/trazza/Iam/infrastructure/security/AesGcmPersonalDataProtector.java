package com.trazza.platform.iam.infrastructure.security;

import com.trazza.platform.iam.application.internal.outboundservices.security.PersonalDataProtector;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256-GCM encryption stored as {@code nonce (12) + ciphertext + tag (16)}, and HMAC-SHA256 of the DNI.
 * The layout is the same as the .NET version, so both can read the same rows.
 */
@Service
public class AesGcmPersonalDataProtector implements PersonalDataProtector {

    private static final int NONCE_SIZE = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();
    private final SecretKeySpec encryptionKey;
    private final SecretKeySpec hashKey;

    public AesGcmPersonalDataProtector(PersonalDataSettings settings) {
        var encryption = decodeKey(settings.encryptionKey());
        var hash = decodeKey(settings.hashKey());
        this.encryptionKey = new SecretKeySpec(encryption, "AES");
        this.hashKey = new SecretKeySpec(hash, "HmacSHA256");
    }

    private static byte[] decodeKey(String base64) {
        try {
            var key = Base64.getDecoder().decode(base64 == null ? "" : base64.trim());
            if (key.length == 32) return key;
        } catch (IllegalArgumentException ignored) {
            // reported below
        }
        throw new IllegalStateException("trazza.personal-data keys must be Base64 keys of 32 bytes "
                + "(PERSONAL_DATA_ENCRYPTION_KEY and PERSONAL_DATA_HASH_KEY).");
    }

    @Override
    public byte[] encrypt(String plainText) {
        try {
            var nonce = new byte[NONCE_SIZE];
            RANDOM.nextBytes(nonce);
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(TAG_BITS, nonce));
            var cipherAndTag = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            var result = new byte[NONCE_SIZE + cipherAndTag.length];
            System.arraycopy(nonce, 0, result, 0, NONCE_SIZE);
            System.arraycopy(cipherAndTag, 0, result, NONCE_SIZE, cipherAndTag.length);
            return result;
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Personal data could not be encrypted.", exception);
        }
    }

    @Override
    public String decrypt(byte[] data) {
        try {
            var nonce = Arrays.copyOfRange(data, 0, NONCE_SIZE);
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(TAG_BITS, nonce));
            var plain = cipher.doFinal(data, NONCE_SIZE, data.length - NONCE_SIZE);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Personal data could not be decrypted (check the shared keys).", exception);
        }
    }

    @Override
    public byte[] hashDni(String dni) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(hashKey);
            return mac.doFinal(dni.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
