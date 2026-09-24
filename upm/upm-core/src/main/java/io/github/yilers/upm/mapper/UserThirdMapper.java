package io.github.yilers.upm.mapper;

import io.github.yilers.upm.entity.UserThird;
import io.github.yilers.web.mybatis.CustomMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface UserThirdMapper extends CustomMapper<UserThird> {

    /**
     * 物理删除绑定关系。tenantId必须来自服务端登录上下文，不能接收客户端传值。
     */
    @Delete("DELETE FROM upm_user_third WHERE tenant_id = #{tenantId} AND user_id = #{userId} AND platform = #{platform}")
    int physicallyDelete(@Param("tenantId") Long tenantId,
                         @Param("userId") Long userId,
                         @Param("platform") String platform);
}
