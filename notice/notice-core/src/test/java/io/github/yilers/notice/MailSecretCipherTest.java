package io.github.yilers.notice;

import static org.junit.jupiter.api.Assertions.*;

import io.github.yilers.notice.config.NoticeProperties;
import io.github.yilers.notice.support.MailSecretCipher;
import io.github.yilers.web.exception.CommonException;

import org.junit.jupiter.api.Test;

import java.util.Base64;

/** 加密边界：随机IV、错误密钥、篡改数据。 */
class MailSecretCipherTest {
    @Test
    void roundTripsWithRandomIvAndRejectsInvalidKeys() {
        var properties = new NoticeProperties();
        properties.setSecretKey(Base64.getEncoder().encodeToString(new byte[32]));
        var cipher = new MailSecretCipher(properties);
        String first = cipher.encrypt("secret"), second = cipher.encrypt("secret");
        assertNotEquals(first, second);
        assertEquals("secret", cipher.decrypt(first));
        byte[] bytes = Base64.getDecoder().decode(first);
        bytes[bytes.length - 1] ^= 1;
        assertThrows(CommonException.class, () -> cipher.decrypt(Base64.getEncoder().encodeToString(bytes)));
        properties.setSecretKey("invalid");
        assertThrows(CommonException.class, () -> cipher.encrypt("secret"));
    }
}
