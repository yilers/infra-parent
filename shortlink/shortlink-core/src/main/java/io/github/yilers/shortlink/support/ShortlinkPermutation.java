package io.github.yilers.shortlink.support;

import io.github.yilers.shortlink.i18n.ShortlinkMessage;
import io.github.yilers.web.exception.CommonException;
import org.bouncycastle.crypto.fpe.FPEFF1Engine;
import org.bouncycastle.crypto.params.FPEParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * FF1按Base62格式作一一映射：相同密钥、长度下不同序号必然得到不同短码。
 * 每次调用创建独立引擎，避免跨线程共享可变密码对象。
 */
@Component
public class ShortlinkPermutation {
    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int RADIX = 62;
    private static final int KEY_BYTES = 32;
    private static final byte[] EMPTY_TWEAK = new byte[0];

    public String permute(long sequence, int length, byte[] key) {
        if (key == null || key.length != KEY_BYTES) {
            throw new CommonException(ShortlinkMessage.PERMUTATION_KEY_LENGTH);
        }
        String encoded = ShortlinkRules.encode(sequence, length);
        byte[] digits = new byte[length];
        byte[] result = new byte[length];
        for (int i = 0; i < length; i++) {
            digits[i] = (byte) ALPHABET.indexOf(encoded.charAt(i));
        }
        try {
            FPEFF1Engine engine = new FPEFF1Engine();
            engine.init(true, new FPEParameters(new KeyParameter(key), RADIX, EMPTY_TWEAK));
            engine.processBlock(digits, 0, length, result, 0);
            StringBuilder output = new StringBuilder(length);
            for (byte digit : result) {
                output.append(ALPHABET.charAt(Byte.toUnsignedInt(digit)));
            }
            return output.toString();
        } catch (RuntimeException e) {
            throw new CommonException(ShortlinkMessage.PERMUTATION_FAILED);
        } finally {
            Arrays.fill(digits, (byte) 0);
            Arrays.fill(result, (byte) 0);
        }
    }
}
