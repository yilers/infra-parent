package io.github.yilers.web.mybatis;

import lombok.Data;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/** 公共填充器只处理约定类型，不能把第三方同名时间字段写成错误类型。 */
class MyMetaObjectHandlerTest {

    private final MyMetaObjectHandler handler = new MyMetaObjectHandler();

    @Test
    void fillsLocalDateTimeAndPreservesExplicitValue() {
        LocalTimeEntity entity = new LocalTimeEntity();
        handler.updateFill(SystemMetaObject.forObject(entity));
        assertNotNull(entity.getUpdateTime());
        LocalDateTime explicit = LocalDateTime.of(2026, 10, 19, 9, 0);
        entity.setUpdateTime(explicit);
        handler.updateFill(SystemMetaObject.forObject(entity));
        assertEquals(explicit, entity.getUpdateTime());
    }

    @Test
    void skipsDateFieldAndEntityWithoutUpdateTime() {
        DateEntity entity = new DateEntity();
        assertDoesNotThrow(() -> handler.updateFill(SystemMetaObject.forObject(entity)));
        assertNull(entity.getUpdateTime());
        Date explicit = new Date();
        entity.setUpdateTime(explicit);
        handler.updateFill(SystemMetaObject.forObject(entity));
        assertEquals(explicit, entity.getUpdateTime());
        assertDoesNotThrow(() -> handler.updateFill(SystemMetaObject.forObject(new Object())));
    }

    @Data
    static class LocalTimeEntity {
        private LocalDateTime updateTime;
    }

    @Data
    static class DateEntity {
        private Date updateTime;
    }
}
