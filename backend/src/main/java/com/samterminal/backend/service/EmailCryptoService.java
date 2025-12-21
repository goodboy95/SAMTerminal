package com.samterminal.backend.service;

import com.samterminal.backend.config.EmailVerificationProperties;
import com.samterminal.backend.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class EmailCryptoService {
    private static final String AES_ALG = "AES";
    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    private final SecretKey encryptionKey;
    private final String codeHashSalt;
    private final SecureRandom secureRandom = new SecureRandom();

    public EmailCryptoService(EmailVerificationProperties properties,
                              @Value("${security.jwt.secret:}") String jwtSecret) {
        String keyBase64 = properties.getEncryption().getKeyBase64();
        byte[] keyBytes;
        if (keyBase64 != null && !keyBase64.isBlank()) {
            keyBytes = Base64.getDecoder().decode(keyBase64);
        } else if (jwtSecret != null && !jwtSecret.isBlank()) {
            keyBytes = sha256(jwtSecret.getBytes(StandardCharsets.UTF_8));
        } else {
            keyBytes = sha256("samterminal-email".getBytes(StandardCharsets.UTF_8));
        }
        if (keyBytes.length < 32) {
            keyBytes = padKey(keyBytes);
        }
        this.encryptionKey = new SecretKeySpec(sliceKey(keyBytes, 32), AES_ALG);
        String salt = properties.getEncryption().getCodeHashSalt();
        if (salt == null || salt.isBlank()) {
            salt = Base64.getEncoder().encodeToString(sha256(encryptionKey.getEncoded()));
        }
        this.codeHashSalt = salt;
    }

    public String hashCode(String code) {
        if (code == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "验证码缺失");
        }
        byte[] data = (code + ":" + codeHashSalt).getBytes(StandardCharsets.UTF_8);
        return Base64.getEncoder().encodeToString(sha256(data));
    }

    public String encrypt(String plain) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return "v1:" + Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(cipherText);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "加密失败");
        }
    }

    public String decrypt(String encrypted) {
        try {
            String[] parts = encrypted.split(":");
            if (parts.length != 3) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "密文格式错误");
            }
            byte[] iv = Base64.getDecoder().decode(parts[1]);
            byte[] cipherText = Base64.getDecoder().decode(parts[2]);
            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] plain = cipher.doFinal(cipherText);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "解密失败");
        }
    }

    public String maskCode(String code) {
        if (code == null || code.length() < 4) {
            return "****";
        }
        return code.substring(0, 2) + "****" + code.substring(code.length() - 2);
    }

    private byte[] sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(data);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "加密初始化失败");
        }
    }

    private byte[] padKey(byte[] keyBytes) {
        byte[] padded = new byte[32];
        System.arraycopy(keyBytes, 0, padded, 0, Math.min(keyBytes.length, 32));
        return padded;
    }

    private byte[] sliceKey(byte[] keyBytes, int length) {
        if (keyBytes.length == length) {
            return keyBytes;
        }
        byte[] slice = new byte[length];
        System.arraycopy(keyBytes, 0, slice, 0, length);
        return slice;
    }
}
