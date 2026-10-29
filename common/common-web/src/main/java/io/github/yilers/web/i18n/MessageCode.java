package io.github.yilers.web.i18n;

/**
 * 可国际化消息的定义。业务模块可以指定自己的资源文件，不依赖 UPM 实现。
 */
public interface MessageCode {

    /** 消息资源中的标识。 */
    String getKey();

    /** 缺少资源时使用的默认中文消息。 */
    String getDefaultMessage();

    /** 消息资源路径，使用 classpath 相对路径且不带语言和扩展名。 */
    default String getBasename() {
        return "i18n/common-messages";
    }
}
