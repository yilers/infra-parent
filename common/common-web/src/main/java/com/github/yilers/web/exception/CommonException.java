package com.github.yilers.web.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 自定义异常
 * @author zhanghui
 * @date 2023/8/9 11:43
 */

@Getter
public class CommonException extends RuntimeException {

    private Integer code = HttpStatus.INTERNAL_SERVER_ERROR.value();

    public CommonException(String message) {
        super(message);
    }

    public CommonException() {
        super();
    }

    public CommonException(Integer code, String message) {
        super(message);
        this.code = code;
    }

}
