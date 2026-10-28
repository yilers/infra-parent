package io.github.yilers.shortlink.mapper;

import io.github.yilers.shortlink.entity.ShortlinkDomain;
import io.github.yilers.web.mybatis.CustomMapper;

/** 短链域名数据访问。默认保留租户过滤。 */
public interface ShortlinkDomainMapper extends CustomMapper<ShortlinkDomain> {
    /** 唯一跨租户公开查询：按登记域名定位租户，同时拒绝停用或已删除租户。 */
    @com.baomidou.mybatisplus.annotation.InterceptorIgnore(tenantLine = "true")
    @org.apache.ibatis.annotations.Select("""
        SELECT * FROM shortlink_domain WHERE host=#{host} AND usable=1 AND deleted=0
        """)
    ShortlinkDomain findForRedirect(String host);
}
