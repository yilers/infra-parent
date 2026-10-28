package io.github.yilers.auth.api;

import cn.dev33.satoken.sign.config.SaSignConfig;
import cn.dev33.satoken.sign.template.SaSignTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 系统调用签名协议v1。仅签固定字段，避免任意参数拼接产生歧义。
 * 正文按实际发送的UTF-8字节摘要；URL查询串首版禁止使用，参数均放在JSON正文。
 */
public final class ApiSignProtocol {
    public static final String PREFIX = "/open-api/";
    public static final String ACCESS_KEY = "X-Api-Key";
    public static final String TIMESTAMP = "X-Api-Timestamp";
    public static final String NONCE = "X-Api-Nonce";
    public static final String SIGNATURE = "X-Api-Signature";
    public static final long WINDOW_MILLIS = 300_000L;
    public static final int MAX_BODY_BYTES = 1_048_576;

    private ApiSignProtocol() {
    }

    public static SaSignTemplate template(String secret) {
        // 使用Sa-Token签名排序及摘要扩展，不使用其默认MD5算法，也不输出带密钥的拼接原文。
        SaSignConfig config = new SaSignConfig(secret).setTimestampDisparity(WINDOW_MILLIS);
        config.setDigestMethod(ApiSignProtocol::sha256);
        return new SaSignTemplate(config) {
            @Override
            public String createSign(Map<String, ?> parameters) {
                // 1.45.0默认实现会debug打印含secret的fullStr；保持签名格式一致，仅移除日志。
                Map<String, Object> values = new java.util.TreeMap<>(parameters);
                values.remove(SaSignTemplate.sign);
                return digestFullStr(joinParamsDictSort(values) + "&key=" + getSecretKey());
            }
        };
    }

    public static Map<String, String> parameters(String accessKey, String method, String path,
                                                  String timestamp, String nonce, byte[] body) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("version", "1");
        values.put("accessKey", accessKey);
        values.put("method", method);
        values.put("pathHash", sha256(path));
        values.put("timestamp", timestamp);
        values.put("nonce", nonce);
        values.put("bodyHash", sha256(body));
        return values;
    }

    public static String sha256(String value) {
        return sha256(value.getBytes(StandardCharsets.UTF_8));
    }

    public static String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("运行环境不支持SHA-256", e);
        }
    }
}
