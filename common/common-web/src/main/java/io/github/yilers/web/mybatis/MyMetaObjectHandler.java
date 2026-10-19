package io.github.yilers.web.mybatis;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import io.github.yilers.web.context.RequestContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now()); // 起始版本 3.3.0(推荐使用)
        // 或者
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class); // 起始版本 3.3.3(推荐)
//        // 或者
//        this.fillStrategy(metaObject, "createTime", LocalDateTime.now()); // 也可以使用(3.3.0 该方法有bug)
        this.strictInsertFill(metaObject, "tenantId", Long.class, RequestContextHolder.getTenantId());
        if (metaObject.hasSetter("createId")) {
            try {
                Object createId = this.getFieldValByName("createId", metaObject);
                // 已有值不填充
                if (ObjectUtil.isNotNull(createId)) {
                    return;
                }
                this.strictInsertFill(metaObject, "createId", Long.class, StpUtil.getLoginIdAsLong());
            } catch (Exception ignored) {
                log.warn("自动填充创建人 获取用户ID失败");
            }
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 公共填充器会作用于第三方Mapper。仅填充本项目的LocalDateTime字段，
        // Warm-Flow等使用Date的实体由其自身维护，不能按同名字段直接赋值。
        if (metaObject.hasGetter("updateTime") && metaObject.hasSetter("updateTime")
                && LocalDateTime.class.equals(metaObject.getSetterType("updateTime"))) {
            this.fillStrategy(metaObject, "updateTime", LocalDateTime.now());
        }
    }
}
