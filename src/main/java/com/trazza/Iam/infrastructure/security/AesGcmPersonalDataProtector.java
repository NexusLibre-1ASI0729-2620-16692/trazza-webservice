package com.trazza.iam.infrastructure.security;

import com.trazza.iam.application.internal.outboundservices.PersonalDataProtector;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption stored as nonce (12) + ciphertext + tag (16), and HMAC-SHA256 of the DNI.
 */
@Service
public class AesGcmPersonalDataProtector implements PersonalDataProtector {

    private static final int NONCE_SIZE = 12;
    private static final int TAG_SIZE = 16;
    private static final int TAG_BITS = TAG_SIZE * 8;

    private final byte[] encryptionKey;
    private final byte[] hashKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesGcmPersonalDataProtector(PersonalDataSettings settings) {
        this.encryptionKey = Base64.getDecoder().decode(settings.getEncryptionKey());
        this.hashKey = Base64.getDecoder().decode(settings.getHashKey());

        if (this.encryptionKey.length != 32 || this.hashKey.length != 32) {
            throw new IllegalStateException("PersonalData keys must be 32 bytes encoded in Base64.");
        }
    }

    @Override
    public byte[] encrypt(String plainText) {
        try {
            byte[] plain = plainText.getBytes(StandardCharsets.UTF_8);
            byte[] nonce = new byte[NONCE_SIZE];
            secureRandom.nextBytes(nonce);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(encryptionKey, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_BITS, nonce);

            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
            byte[] cipherTextWithTag = cipher.doFinal(plain);

            // El resultado en Java ya incluye el tag al final (ciphertext + tag)
            // Estructura final: nonce (12) + ciphertext + tag (16)
            byte[] result = new byte[NONCE_SIZE + cipherTextWithTag.length];
            System.arraycopy(nonce, 0, result, 0, NONCE_SIZE);
            System.arraycopy(cipherTextWithTag, 0, result, NONCE_SIZE, cipherTextWithTag.length);

            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Error encrypting personal data", e);
        }
    }

    @Override
    public String decrypt(byte[] cipherData) {
        try {
            if (cipherData.length < NONCE_SIZE + TAG_SIZE) {
                throw new IllegalArgumentException("Invalid cipher data length");
            }

            byte[] nonce = new byte[NONCE_SIZE];
            System.arraycopy(cipherData, 0, nonce, 0, NONCE_SIZE);

            int cipherTextLength = cipherData.length - NONCE_SIZE;
            byte[] cipherTextWithTag = new byte[cipherTextLength];
            System.arraycopy(cipherData, NONCE_SIZE, cipherTextWithTag, 0, cipherTextLength);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(encryptionKey, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_BITS, nonce);

            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
            byte[] plain = cipher.doFinal(cipherTextWithTag);

            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Error decrypting personal data", e);
        }
    }

    @Override
    public byte[] hashDni(String dni) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(hashKey, "HmacSHA256");
            mac.init(keySpec);
            return mac.doFinal(dni.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Error hashing DNI", e);
        }
    }
}