package io.github.yilers.web.i18n;

import java.util.Map;

/** 为尚保留原始文本的校验结果提供可翻译的消息定义。 */
public interface MessageTextCatalog {

    Map<String, MessageCode> messages();
}
