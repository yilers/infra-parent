package io.github.yilers.notice.support;

import io.github.yilers.notice.config.NoticeProperties;
import io.github.yilers.notice.i18n.NoticeMessageCode;
import io.github.yilers.web.exception.CommonException;

import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** SMTP授权码采用独立主密钥AES-256-GCM加密，随机IV，不返回明文或密文。 */
@Component
public class MailSecretCipher {
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private final NoticeProperties properties;
    private final SecureRandom random = new SecureRandom();

    public MailSecretCipher(NoticeProperties properties) {
        this.properties = properties;
    }

    public String encrypt(String value) {
        return crypt(value, true);
    }

    public String decrypt(String value) {
        return crypt(value, false);
    }

    private String crypt(String value, boolean encrypt) {
        byte[] key;
        try {
            key = Base64.getDecoder().decode(properties.getSecretKey());
        } catch (IllegalArgumentException e) {
            throw new CommonException(NoticeMessageCode.KEY_INVALID);
        }
        if (key.length != 32) throw new CommonException(NoticeMessageCode.KEY_INVALID);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            if (encrypt) {
                byte[] iv = new byte[IV_BYTES];
                random.nextBytes(iv);
                cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
                byte[] data = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
                return Base64.getEncoder()
                        .encodeToString(ByteBuffer.allocate(iv.length + data.length).put(iv).put(data).array());
            }
            byte[] data = Base64.getDecoder().decode(value);
            if (data.length <= IV_BYTES) throw new IllegalArgumentException();
            ByteBuffer buffer = ByteBuffer.wrap(data);
            byte[] iv = new byte[IV_BYTES];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new CommonException(NoticeMessageCode.SECRET_FAILED);
        } finally {
            java.util.Arrays.fill(key, (byte) 0);
        }
    }
}
