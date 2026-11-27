package io.github.yilers.notice.service;

import io.github.yilers.notice.model.NoticeTarget;
import io.github.yilers.notice.model.NoticeUser;

import java.util.List;

/** 通知接收范围解析契约。 */
public interface NoticeAudienceService {
    /** 校验用户、角色和部门范围，解析并去重接收用户。 */
    List<NoticeUser> resolve(NoticeTarget target);
}
