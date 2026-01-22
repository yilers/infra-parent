package io.github.yilers.core.util;

import java.io.Serializable;


/**
 * 统一响应实体
 *
 * @param <T> 响应数据类型
 * @author yilers
 */

@SuppressWarnings("all")
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 响应业务状态 */
    private Integer code;

    /** 子响应业务状态，可选 */
    private Integer subCode;

    /** 响应消息 */
    private String msg;

    /** 响应中的数据 */
    private T data;

    // ===================== 构造器 =====================

    public Result() {
    }

    public Result(T data) {
        this.code = 200;
        this.msg = "OK";
        this.data = data;
    }

    public Result(Integer code, String msg) {
        this(code, msg, null);
    }

    public Result(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    // ===================== 静态构建方法 =====================

    public static <T> Result<T> ok() {
        return new Result<>(null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(data);
    }

    public static <T> Result<T> build(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }

    public static <T> Result<T> build(Integer code, String msg, T data) {
        return new Result<>(code, msg, data);
    }

    public static <T> Result<T> fail(String msg) {
        return new Result<>(500, msg, null);
    }

    public static <T> Result<T> fail(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }

    // ===================== 判断方法 =====================

    /**
     * 判断是否成功
     */
    public boolean success() {
        return Integer.valueOf(200).equals(this.code);
    }

    /**
     * 判断是否失败
     */
    public boolean fail() {
        return !success();
    }

    // ===================== 链式 setter =====================

    public Result<T> code(Integer code) {
        this.code = code;
        return this;
    }

    public Result<T> subCode(Integer subCode) {
        this.subCode = subCode;
        return this;
    }

    public Result<T> msg(String msg) {
        this.msg = msg;
        return this;
    }

    public Result<T> data(T data) {
        this.data = data;
        return this;
    }

    // ===================== getter/setter =====================

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public Integer getSubCode() {
        return subCode;
    }

    public void setSubCode(Integer subCode) {
        this.subCode = subCode;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "Result{" +
                "code=" + code +
                ", subCode=" + subCode +
                ", msg='" + msg + '\'' +
                ", data=" + data +
                '}';
    }
}
