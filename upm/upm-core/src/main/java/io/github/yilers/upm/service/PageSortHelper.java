package io.github.yilers.upm.service;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.yilers.api.base.BasePageRequest;
import io.github.yilers.upm.i18n.UpmManagementMessage;
import io.github.yilers.web.exception.CommonException;

import java.util.List;
import java.util.Map;

/** 分页排序只接受白名单字段，将其转换为服务端固定的列名。 */
final class PageSortHelper {
    private PageSortHelper() {
    }

    static void apply(Page<?> page, BasePageRequest<?> request, Map<String, String> columns) {
        List<String> fields = request.getSortFieldList();
        List<String> directions = request.getSortOrderList();
        if (fields == null || fields.isEmpty()) {
            if (directions != null && !directions.isEmpty()) {
                throw invalid();
            }
            return;
        }
        if (directions == null || fields.size() != directions.size()) {
            throw invalid();
        }
        for (int index = 0; index < fields.size(); index++) {
            String field = fields.get(index);
            String column = field == null ? null : columns.get(field);
            String direction = directions.get(index);
            if (column == null || !("asc".equalsIgnoreCase(direction) || "desc".equalsIgnoreCase(direction))) {
                throw invalid();
            }
            // 列名来自后端常量，客户端不能提交 SQL 表达式或指定表别名。
            page.addOrder(new OrderItem().setColumn(column).setAsc("asc".equalsIgnoreCase(direction)));
        }
    }

    private static CommonException invalid() {
        return new CommonException(UpmManagementMessage.PAGE_SORT_INVALID);
    }
}
