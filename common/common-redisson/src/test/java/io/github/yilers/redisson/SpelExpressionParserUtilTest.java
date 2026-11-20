package io.github.yilers.redisson;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class SpelExpressionParserUtilTest {
    private static final Method METHOD;

    static {
        try {
            METHOD = Examples.class.getDeclaredMethod("process", String.class, Payload.class);
        } catch (NoSuchMethodException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @Test
    void constantKeysAreParsedAndUnquotedLegacyKeysAreRejected() {
        assertEquals("infra:order", parse("'infra:order'"));
        assertThrows(IllegalArgumentException.class, () -> parse("infra:order"));
    }

    @Test
    void staticMethodWithoutParameterReferenceIsEvaluated() {
        assertEquals("infra:7", parse("'infra:' + T(java.lang.Math).abs(-7)"));
    }

    @Test
    void namedParametersAndObjectPropertiesCanBeUsedTogether() {
        assertEquals("order-1:tenant-1", parse("#orderId + ':' + #request.code"));
    }

    @Test
    void positionalAliasesUseSameArguments() {
        assertEquals("order-1:order-1:order-1:tenant-1",
                parse("#arg0 + ':' + #p0 + ':' + #a0 + ':' + #arg1.code"));
    }

    @Test
    void nullSafeAccessAndElvisFallbackWork() {
        assertEquals("fallback", SpelExpressionParserUtil.parseKey(
                "#request?.code ?: 'fallback'", METHOD, new Object[]{"order-1", null}));
    }

    @Test
    void emptyResultsAndInvalidExpressionsFailInsteadOfFallingBackToLiteralKey() {
        for (String expression : new String[]{"", "   ", "null", "''", "'  '", "#missing", "'unterminated", "#request.missing"}) {
            assertThrows(IllegalArgumentException.class, () -> parse(expression));
        }
    }

    @Test
    void differentExpressionsOnSameMethodHaveIndependentCacheEntries() {
        assertEquals("lock:order-1", parse("'lock:' + #arg0"));
        assertEquals("rate:order-1", parse("'rate:' + #arg0"));
    }

    @Test
    void noArgumentMethodsCanUseStaticExpressions() throws Exception {
        Method method = Examples.class.getDeclaredMethod("empty");
        assertEquals("3", SpelExpressionParserUtil.parseKey("T(java.lang.Math).abs(-3)", method, new Object[0]));
    }

    private String parse(String expression) {
        return SpelExpressionParserUtil.parseKey(expression, METHOD, new Object[]{"order-1", new Payload("tenant-1")});
    }

    public record Payload(String code) {
    }

    public static class Examples {
        public void process(String orderId, Payload request) { }
        public void empty() { }
    }
}
