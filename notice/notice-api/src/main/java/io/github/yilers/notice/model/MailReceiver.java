package io.github.yilers.notice.model;

import java.util.Map;

/** 一个邮件接收目标。外部邮箱不要求关联系统用户。 */
public record MailReceiver(Long userId, String email, String name, Map<String, String> params) {}
