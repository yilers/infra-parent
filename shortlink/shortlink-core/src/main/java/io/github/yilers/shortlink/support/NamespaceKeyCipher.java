package io.github.yilers.shortlink.support;

import io.github.yilers.shortlink.i18n.ShortlinkMessage;
import io.github.yilers.web.exception.CommonException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/** namespace随机密钥使用独立主密钥加密保存，不向前端或日志输出明文。 */
@Component
public class NamespaceKeyCipher {
    private static final int KEY_BYTES = 32;
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private final SecureRandom random = new SecureRandom();
    private final String masterKey;

    public NamespaceKeyCipher(@Value("${shortlink.crypto.master-key:${SHORTLINK_MASTER_KEY:}}") String masterKey) {
        this.masterKey = masterKey;
    }

    /** 每个自动生成路径只生成一次密钥，避免更换映射造成重复。 */
    public String createEncryptedKey() {
        byte[] key = new byte[KEY_BYTES];
        random.nextBytes(key);
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, masterKey(), new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(key);
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + encrypted.length)
                    .put(iv).put(encrypted).array());
        } catch (CommonException e) {
            throw e;
        } catch (Exception e) {
            throw new CommonException(ShortlinkMessage.KEY_ENCRYPT_FAILED);
        } finally {
            Arrays.fill(key, (byte) 0);
        }
    }

    public byte[] decryptKey(String ciphertext) {
        try {
            if (ciphertext == null || ciphertext.isBlank()) {
                throw new CommonException(ShortlinkMessage.KEY_MISSING);
            }
            byte[] value = Base64.getDecoder().decode(ciphertext);
            if (value.length != IV_BYTES + KEY_BYTES + TAG_BITS / Byte.SIZE) {
                throw new CommonException(ShortlinkMessage.KEY_FORMAT_INVALID);
            }
            ByteBuffer buffer = ByteBuffer.wrap(value);
            byte[] iv = new byte[IV_BYTES];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, masterKey(), new GCMParameterSpec(TAG_BITS, iv));
            return cipher.doFinal(encrypted);
        } catch (CommonException e) {
            throw e;
        } catch (Exception e) {
            throw new CommonException(ShortlinkMessage.KEY_DECRYPT_FAILED);
        }
    }

    private SecretKeySpec masterKey() {
        try {
            byte[] decoded = Base64.getDecoder().decode(masterKey);
            if (decoded.length != KEY_BYTES) {
                throw new IllegalArgumentException();
            }
            try {
                return new SecretKeySpec(decoded, "AES");
            } finally {
                Arrays.fill(decoded, (byte) 0);
            }
        } catch (IllegalArgumentException e) {
            throw new CommonException(ShortlinkMessage.MASTER_KEY_INVALID);
        }
    }
}
