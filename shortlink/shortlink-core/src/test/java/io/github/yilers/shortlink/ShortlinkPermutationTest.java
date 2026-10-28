package io.github.yilers.shortlink;

import io.github.yilers.shortlink.support.NamespaceKeyCipher;
import io.github.yilers.shortlink.support.ShortlinkCodeGenerator;
import io.github.yilers.shortlink.support.ShortlinkPermutation;
import io.github.yilers.shortlink.support.ShortlinkRules;
import io.github.yilers.shortlink.entity.ShortlinkNamespace;
import io.github.yilers.shortlink.service.ShortlinkSegmentService;
import io.github.yilers.web.exception.CommonException;
import org.bouncycastle.crypto.fpe.FPEFF1Engine;
import org.bouncycastle.crypto.params.FPEParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/** 验证密钥隔离、固定长度及FF1映射的可逆性和不重复性。 */
class ShortlinkPermutationTest {
    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private final ShortlinkPermutation permutation = new ShortlinkPermutation();

    @Test
    void permutationIsDeterministicReversibleAndUniqueForBothLengths() {
        byte[] key = new byte[32];
        Arrays.fill(key, (byte) 42);
        for (int length : new int[]{6, 8}) {
            Set<String> seen = new HashSet<>();
            for (long value = 0; value < 3000; value++) {
                String code = permutation.permute(value, length, key);
                assertEquals(length, code.length());
                assertTrue(code.matches("[0-9A-Za-z]+"));
                assertTrue(seen.add(code));
                assertEquals(ShortlinkRules.encode(value, length), decrypt(code, key));
            }
            assertEquals(permutation.permute(123, length, key), permutation.permute(123, length, key));
        }
        byte[] otherKey = new byte[32];
        Arrays.fill(otherKey, (byte) 43);
        assertNotEquals(permutation.permute(123, 8, key), permutation.permute(123, 8, otherKey));
    }

    @Test
    void namespaceKeysAreRandomAndRequireTheCorrectMasterKey() {
        String master = Base64.getEncoder().encodeToString(new byte[32]);
        NamespaceKeyCipher cipher = new NamespaceKeyCipher(master);
        String first = cipher.createEncryptedKey();
        String second = cipher.createEncryptedKey();
        assertNotEquals(first, second);
        assertEquals(32, cipher.decryptKey(first).length);
        byte[] wrong = new byte[32];
        wrong[0] = 1;
        NamespaceKeyCipher wrongCipher = new NamespaceKeyCipher(Base64.getEncoder().encodeToString(wrong));
        assertThrows(CommonException.class, () -> wrongCipher.decryptKey(first));
        assertThrows(CommonException.class, () -> new NamespaceKeyCipher("").createEncryptedKey());
        assertThrows(CommonException.class, () -> permutation.permute(1, 6, new byte[1]));
    }

    @Test
    void wrongMasterKeyDoesNotAllocateOrConsumeASequence() {
        NamespaceKeyCipher original = new NamespaceKeyCipher(Base64.getEncoder().encodeToString(new byte[32]));
        byte[] wrong = new byte[32];
        wrong[0] = 1;
        NamespaceKeyCipher invalid = new NamespaceKeyCipher(Base64.getEncoder().encodeToString(wrong));
        ShortlinkSegmentService segments = mock(ShortlinkSegmentService.class);
        ShortlinkCodeGenerator generator = new ShortlinkCodeGenerator(segments, invalid, permutation);
        ShortlinkNamespace namespace = new ShortlinkNamespace();
        namespace.setId(1L);
        namespace.setCodeLength(6);
        namespace.setCodeAlgorithm(ShortlinkRules.ALGORITHM_FF1_V1);
        namespace.setCodeKeyCipher(original.createEncryptedKey());
        assertThrows(CommonException.class, () -> generator.next(namespace));
        verifyNoInteractions(segments);
    }

    private static String decrypt(String code, byte[] key) {
        byte[] input = new byte[code.length()];
        byte[] output = new byte[code.length()];
        for (int i = 0; i < code.length(); i++) {
            input[i] = (byte) ALPHABET.indexOf(code.charAt(i));
        }
        FPEFF1Engine engine = new FPEFF1Engine();
        engine.init(false, new FPEParameters(new KeyParameter(key), 62, new byte[0]));
        engine.processBlock(input, 0, input.length, output, 0);
        StringBuilder value = new StringBuilder(code.length());
        for (byte digit : output) value.append(ALPHABET.charAt(Byte.toUnsignedInt(digit)));
        return value.toString();
    }
}
