package io.github.yilers.web.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.SaTokenException;
import io.github.yilers.core.util.Result;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


/**
 * 全局异常处理
 * @author zhanghui
 * @since 2023/8/2 10:23
 */

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler
    public Result<?> exceptionHandler(Exception e) {
        e.printStackTrace();
        log.error("系统异常: {}", e.getMessage(), e);
        return Result.build(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
    }

    @ExceptionHandler
    public Result<?> notLoginExceptionHandler(NotLoginException e) {
        e.printStackTrace();
        return Result.build(e.getCode(), e.getMessage());
    }

    @ExceptionHandler
    public Result<?> saTokenExceptionHandler(SaTokenException e) {
        e.printStackTrace();
        return Result.build(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(value = CommonException.class)
    public Result<?> commonExceptionHandler(CommonException e) {
        e.printStackTrace();
        return Result.build(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public Object methodArgumentNotValidException(HttpServletResponse response, Exception e) {
        log.error("参数异常验证:{}", e.getMessage());
        int status = HttpStatus.BAD_REQUEST.value();
        response.setStatus(status);
        MethodArgumentNotValidException ex = (MethodArgumentNotValidException) e;
        return Result.build(status, ex.getBindingResult().getFieldError().getDefaultMessage());
    }

    @ExceptionHandler(value = BindException.class)
    public Result<?> bindExceptionHandler(HttpServletResponse response, Exception e) {
        log.error("参数异常验证:{}", e.getMessage());
        int status = HttpStatus.BAD_REQUEST.value();
        response.setStatus(status);
        BindException ex = (BindException) e;
        return Result.build(status, ex.getBindingResult().getFieldError().getDefaultMessage());
    }

//    @ExceptionHandler(value = RateLimitException.class)
//    public Result<?> rateLimiterHandler(Exception e) {
//        e.printStackTrace();
//        return Result.build(HttpStatus.TOO_MANY_REQUESTS.value(), e.getMessage());
//    }

}
