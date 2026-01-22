package io.github.yilers.web.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.List;

@Data
public class BasePageRequest<T> {

    @Schema(example = "1")
    private Long current = 1L;

    @Schema(example = "20")
    private Long size = 20L;

    @Schema(description = "开始查询时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private String startTime;

    @Schema(description = "结束查询时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private String endTime;

    @Schema(description = "排序字段 eg:create_time")
    private List<String> sortFieldList;

    @Schema(description = "排序方式 eg:asc 与sortFieldList保持对应")
    private List<String> sortOrderList;

    @Schema(description = "请求条件")
    private T data;
}
