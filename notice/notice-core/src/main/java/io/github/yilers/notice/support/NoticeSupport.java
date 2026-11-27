package io.github.yilers.notice.support;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;

import io.github.yilers.notice.entity.NoticeEntity;
import io.github.yilers.notice.i18n.NoticeMessageCode;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.web.exception.CommonException;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;

import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

/** 可信上下文、JSON及参数边界；不允许使用客户端提供的租户ID覆盖上下文。 */
@Component
public class NoticeSupport {
    private final ObjectMapper json;

    public NoticeSupport(ObjectMapper json) {
        this.json = json;
    }

    /** 校验单个邮箱地址，拒绝显示名称、地址列表和换行。 */
    public static String email(String value) {
        try {
            require(
                    value != null && value.length() <= 255 && !value.contains("\r") && !value.contains("\n"),
                    NoticeMessageCode.INVALID,
                    "email");
            InternetAddress address = new InternetAddress(value, true);
            address.validate();
            require(address.getAddress().equals(value) && value.contains("@"), NoticeMessageCode.INVALID, "email");
            return value;
        } catch (AddressException e) {
            throw new CommonException(NoticeMessageCode.INVALID, "email");
        }
    }

    public static Long tenant() {
        Long id = RequestContextHolder.getTenantId();
        if (id == null) throw new CommonException(NoticeMessageCode.TENANT_REQUIRED);
        return id;
    }

    public static Long user() {
        Long id = RequestContextHolder.getUserId();
        if (id == null) throw new CommonException(NoticeMessageCode.LOGIN_REQUIRED);
        return id;
    }

    public static void require(boolean valid, NoticeMessageCode message, Object... args) {
        if (!valid) throw new CommonException(message, args);
    }

    public static String text(String value, int max, String field) {
        require(value != null && !value.isBlank() && value.length() <= max, NoticeMessageCode.INVALID, field);
        return value.trim();
    }

    public static String optional(String value, int max, String field) {
        require(value == null || value.length() <= max, NoticeMessageCode.INVALID, field);
        return value == null || value.isBlank() ? null : value;
    }

    public static void page(int current, int size) {
        require(current > 0 && size > 0 && size <= 100, NoticeMessageCode.INVALID, "page");
    }

    public static <E extends Enum<E>> E type(Class<E> type, String value) {
        try {
            return Enum.valueOf(type, value);
        } catch (RuntimeException e) {
            throw new CommonException(NoticeMessageCode.INVALID, type.getSimpleName());
        }
    }

    public static void initialize(NoticeEntity entity) {
        entity.setId(IdWorker.getId());
        entity.setTenantId(tenant());
        entity.setCreateId(RequestContextHolder.getUserId());
        entity.setVersion(1);
        entity.setDeleted(0);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(entity.getCreateTime());
    }

    public static void version(NoticeEntity entity, Integer version) {
        tenant();
        require(entity != null, NoticeMessageCode.NOT_FOUND);
        require(version != null && version.equals(entity.getVersion()), NoticeMessageCode.CONFLICT);
    }

    public String fingerprint(Object value) {
        try {
            Object tree = json.readValue(json.writeValueAsString(value), Object.class);
            byte[] bytes =
                    java.security.MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(canonical(tree)));
            return java.util.HexFormat.of().formatHex(bytes);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private Object canonical(Object value) {
        if (value instanceof java.util.Map<?, ?> map) {
            java.util.Map<String, Object> sorted = new java.util.TreeMap<>();
            map.forEach((k, v) -> sorted.put(k.toString(), canonical(v)));
            return sorted;
        }
        if (value instanceof java.util.List<?> list) return list.stream().map(this::canonical).toList();
        return value;
    }

    public String json(Object value) {
        return json.writeValueAsString(value);
    }

    public <T> T read(String value, Class<T> type) {
        return json.readValue(value, type);
    }

    public <T> List<T> list(String value, Class<T> type) {
        if (value == null || value.isBlank()) return List.of();
        return json.readValue(value, json.getTypeFactory().constructCollectionType(List.class, type));
    }

    public java.util.Map<String, String> params(String value) {
        if (value == null || value.isBlank()) return java.util.Map.of();
        return json.readValue(
                value,
                json.getTypeFactory().constructMapType(java.util.LinkedHashMap.class, String.class, String.class));
    }
}
