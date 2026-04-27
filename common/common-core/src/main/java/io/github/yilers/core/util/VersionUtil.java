package io.github.yilers.core.util;

import lombok.experimental.UtilityClass;

import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

@UtilityClass
public class VersionUtil {

    private static final String DEFAULT_VERSION = "dev";
    private static final String DEFAULT_TIME = "";

    // 懒加载缓存
    private static final AtomicReference<VersionInfo> CACHE = new AtomicReference<>();


    public static String getVersion() {
        return get().version;
    }

    public static String getTime() {
        return get().time;
    }

    // ===== 核心逻辑 =====

    private static VersionInfo get() {
        VersionInfo info = CACHE.get();
        if (info != null) {
            return info;
        }
        info = load();
        // CAS 保证只初始化一次（线程安全）
        if (CACHE.compareAndSet(null, info)) {
            return info;
        }
        return CACHE.get();
    }

    private static VersionInfo load() {
        Properties props = new Properties();

        try (InputStream is = VersionUtil.class
                .getClassLoader()
                .getResourceAsStream("version.properties")) {

            if (is != null) {
                props.load(is);
            }

        } catch (Exception ignored) {
        }

        String version = clean(props.getProperty("version"), DEFAULT_VERSION);
        String time = clean(props.getProperty("time"), DEFAULT_TIME);

        return new VersionInfo(version, time);
    }

    /**
     * 清洗未被 Maven 替换的占位符
     */
    private static String clean(String value, String def) {
        if (value == null) {
            return def;
        }
        String v = value.trim();
        if (v.contains("${") || v.contains("@")) {
            return def;
        }
        return v;
    }

    // ===== 内部对象 =====

    private static class VersionInfo {
        final String version;
        final String time;

        VersionInfo(String version, String time) {
            this.version = version;
            this.time = time;
        }
    }
}