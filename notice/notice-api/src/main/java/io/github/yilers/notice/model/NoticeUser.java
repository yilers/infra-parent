package io.github.yilers.notice.model;

import io.swagger.v3.oas.annotations.media.Schema;

/** 宿主提供的同租户有效用户基本信息，不依赖UPM实体。 */
@Schema(description = "通知用户基础信息")
public record NoticeUser(Long id, String name, String email) {}
