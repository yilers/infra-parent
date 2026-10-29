package io.github.yilers.web.exception;

import io.github.yilers.web.i18n.MessageCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.text.MessageFormat;

/**
 * 自定义异常
 * @author zhanghui
 * @since 2023/8/9 11:43
 */

@Getter
public class CommonException extends RuntimeException {

    private Integer code = HttpStatus.INTERNAL_SERVER_ERROR.value();

    /** 国际化消息定义；旧式字符串异常不设置该值。 */
    private MessageCode messageCode;

    /** 消息模板参数。 */
    private Object[] messageArguments;

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

    /** 使用默认状态码和可国际化消息；日志及旧调用方仍可读取默认中文。 */
    public CommonException(MessageCode messageCode, Object... arguments) {
        super(MessageFormat.format(messageCode.getDefaultMessage(), arguments));
        this.messageCode = messageCode;
        this.messageArguments = arguments.clone();
    }

    /** 使用指定状态码和可国际化消息。 */
    public CommonException(Integer code, MessageCode messageCode, Object... arguments) {
        this(messageCode, arguments);
        this.code = code;
    }

}
