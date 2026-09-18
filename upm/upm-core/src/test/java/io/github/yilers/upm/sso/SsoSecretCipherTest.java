package io.github.yilers.upm.sso;

import io.github.yilers.web.exception.CommonException;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class SsoSecretCipherTest {

    @Test
    void encryptsWithRandomIvAndDecrypts() {
        SsoSecretCipher cipher = cipher(randomMasterKey());

        String first = cipher.encrypt("client-secret");
        String second = cipher.encrypt("client-secret");

        assertNotEquals(first, second);
        assertEquals("client-secret", cipher.decrypt(first));
        assertEquals("client-secret", cipher.decrypt(second));
    }

    @Test
    void rejectsMissingOrInvalidMasterKey() {
        assertThrows(CommonException.class, () -> cipher("").encrypt("secret"));
        assertThrows(CommonException.class, () -> cipher("not-base64").encrypt("secret"));
    }

    private SsoSecretCipher cipher(String masterKey) {
        return new SsoSecretCipher(masterKey);
    }

    private String randomMasterKey() {
        byte[] value = new byte[32];
        new SecureRandom().nextBytes(value);
        return Base64.getEncoder().encodeToString(value);
    }
}
