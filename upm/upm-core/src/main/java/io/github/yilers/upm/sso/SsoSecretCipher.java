package io.github.yilers.upm.sso;

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

/**
 * 使用 AES-256-GCM 加解密 SSO 客户端密钥。
 * 主密钥只能从部署环境注入，不允许写入数据库或源码。
 */
@Component
public class SsoSecretCipher {
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;

    private final SecureRandom secureRandom = new SecureRandom();

    private final String masterKey;

    public SsoSecretCipher(@Value("${upm.sso.secret-key:}") String masterKey) {
        this.masterKey = masterKey;
    }

    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + encrypted.length)
                    .put(iv).put(encrypted).array());
        } catch (CommonException e) {
            throw e;
        } catch (Exception e) {
            throw new CommonException("SSO客户端密钥加密失败");
        }
    }

    public String decrypt(String ciphertext) {
        try {
            byte[] value = Base64.getDecoder().decode(ciphertext);
            if (value.length <= IV_LENGTH) {
                throw new CommonException("SSO客户端密钥格式错误");
            }
            ByteBuffer buffer = ByteBuffer.wrap(value);
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_LENGTH, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (CommonException e) {
            throw e;
        } catch (Exception e) {
            throw new CommonException("SSO客户端密钥解密失败，请检查主密钥配置");
        }
    }

    private SecretKeySpec key() {
        if (masterKey == null || masterKey.isBlank()) {
            throw new CommonException("未配置UPM_SSO_SECRET_KEY，无法使用SSO客户端密钥");
        }
        byte[] value;
        try {
            value = Base64.getDecoder().decode(masterKey);
        } catch (IllegalArgumentException e) {
            throw new CommonException("UPM_SSO_SECRET_KEY必须是Base64编码");
        }
        if (value.length != 32) {
            throw new CommonException("UPM_SSO_SECRET_KEY解码后必须为32字节");
        }
        return new SecretKeySpec(value, "AES");
    }
}
