package io.github.yilers.upm.credential;

import io.github.yilers.upm.entity.ApiCredential;
import io.github.yilers.upm.response.ApiCredentialSecretResponse;
import io.github.yilers.web.exception.CommonException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/** 调用密钥加密和响应脱敏测试。 */
class ApiSecretCipherTest {
    @Test
    void encryptedSecretRequiresCorrectMasterKeyAndUsesRandomIv() {
        byte[] bytes = new byte[32];
        Arrays.fill(bytes, (byte) 11);
        ApiSecretCipher cipher = new ApiSecretCipher(Base64.getEncoder().encodeToString(bytes));
        String first = cipher.encrypt("secret-value");
        String second = cipher.encrypt("secret-value");
        assertNotEquals(first, second);
        assertEquals("secret-value", cipher.decrypt(first));
        bytes[0] = 12;
        ApiSecretCipher wrongKey = new ApiSecretCipher(Base64.getEncoder().encodeToString(bytes));
        assertThrows(CommonException.class, () -> wrongKey.decrypt(first));
        assertThrows(CommonException.class, () -> new ApiSecretCipher("invalid").encrypt("secret-value"));
    }

    @Test
    void credentialListAndToStringNeverExposeEncryptedOrPlainSecret() {
        ApiCredential credential = new ApiCredential();
        credential.setSecretCipher("encrypted-secret");
        assertFalse(JsonMapper.builder().build().writeValueAsString(credential).contains("encrypted-secret"));
        assertFalse(credential.toString().contains("encrypted-secret"));
        assertFalse(new ApiCredentialSecretResponse(1L, "key", "plain-secret").toString().contains("plain-secret"));
    }
}
