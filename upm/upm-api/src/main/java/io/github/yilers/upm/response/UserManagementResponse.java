package io.github.yilers.upm.response;

import io.github.yilers.api.field.FieldMaskType;
import io.github.yilers.api.field.FieldPermission;
import io.swagger.v3.oas.annotations.media.Schema;

/** 用户管理专用响应；登录、个人中心及用户缓存仍使用未标记的UserInfoResponse。 */
@Schema(description = "用户管理响应，手机号和邮箱按字段权限显示")
public class UserManagementResponse extends UserInfoResponse {
    @Override
    @FieldPermission(resource = "system:user", resourceName = "用户资料", field = "phone", name = "手机号",
            mask = FieldMaskType.PHONE, writeSupported = true)
    public String getPhone() {
        return super.getPhone();
    }

    @Override
    @FieldPermission(resource = "system:user", resourceName = "用户资料", field = "email", name = "邮箱",
            mask = FieldMaskType.EMAIL, writeSupported = true)
    public String getEmail() {
        return super.getEmail();
    }
}
