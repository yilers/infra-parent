package io.github.yilers.shortlink.support;

import io.github.yilers.shortlink.i18n.ShortlinkMessage;
import io.github.yilers.web.exception.CommonException;
import java.net.URI;

/** 短链固定规则。短码不承载授权语义，敏感目标仍需业务系统鉴权。 */
public final class ShortlinkRules {
    public static final String AUTO = "AUTO";
    public static final String CUSTOM = "CUSTOM";
    public static final String ALGORITHM_SEQUENTIAL = "SEQUENTIAL";
    public static final String ALGORITHM_FF1_V1 = "FF1_V1";
    public static final String ALGORITHM_NONE = "NONE";
    public static final String URL = "URL";
    public static final String TEMPLATE = "TEMPLATE";
    public static final int ENABLED = 1;
    public static final int DISABLED = 0;
    public static final int INITIAL_VERSION = 1;
    public static final int BATCH_LIMIT = 1000;
    public static final int SEGMENT_SIZE = 1000;
    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private ShortlinkRules() {
    }

    public static long capacity(int length) {
        if (length != 6 && length != 8) throw new CommonException(ShortlinkMessage.CODE_LENGTH_INVALID);
        long result = 1;
        for (int i = 0; i < length; i++) result = Math.multiplyExact(result, ALPHABET.length());
        return result;
    }

    public static String encode(long value, int length) {
        if (value < 0 || value >= capacity(length)) throw new CommonException(ShortlinkMessage.CODE_SPACE_EXHAUSTED);
        char[] code = new char[length];
        for (int i = length - 1; i >= 0; i--) {
            code[i] = ALPHABET.charAt((int) (value % ALPHABET.length()));
            value /= ALPHABET.length();
        }
        return new String(code);
    }

    public static String validateUrl(String value) {
        try {
            URI uri = URI.create(value);
            if (value.length() > 4096 || value.chars().anyMatch(Character::isISOControl)
                    || !("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getRawUserInfo() != null) {
                throw new IllegalArgumentException();
            }
            return value;
        } catch (Exception e) {
            throw new CommonException(ShortlinkMessage.TARGET_URL_INVALID);
        }
    }
}
