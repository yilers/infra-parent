package io.github.yilers.upm.credential;

import io.github.yilers.web.exception.CommonException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/** 系统调用密钥AES-256-GCM存储；独立主密钥从环境注入，与SSO密钥隔离。 */
@Component
public class ApiSecretCipher {
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final int KEY_BYTES = 32;
    private final SecureRandom random = new SecureRandom();
    private final String masterKey;

    public ApiSecretCipher(@Value("${upm.api.secret-key:}") String masterKey) {
        this.masterKey = masterKey;
    }

    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, iv));
            byte[] value = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + value.length)
                    .put(iv).put(value).array());
        } catch (CommonException e) {
            throw e;
        } catch (Exception e) {
            throw new CommonException("系统调用密钥加密失败");
        }
    }

    public String decrypt(String ciphertext) {
        try {
            byte[] value = Base64.getDecoder().decode(ciphertext);
            if (value.length <= IV_BYTES + TAG_BITS / Byte.SIZE) {
                throw new CommonException("系统调用密钥格式错误");
            }
            ByteBuffer buffer = ByteBuffer.wrap(value);
            byte[] iv = new byte[IV_BYTES];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (CommonException e) {
            throw e;
        } catch (Exception e) {
            throw new CommonException("系统调用密钥解密失败，请检查主密钥配置");
        }
    }

    private SecretKeySpec key() {
        try {
            byte[] bytes = Base64.getDecoder().decode(masterKey);
            if (bytes.length != KEY_BYTES) {
                throw new IllegalArgumentException();
            }
            return new SecretKeySpec(bytes, "AES");
        } catch (IllegalArgumentException e) {
            throw new CommonException("upm.api.secret-key必须配置为Base64编码的32字节随机主密钥");
        }
    }
}
