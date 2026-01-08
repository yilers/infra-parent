package com.github.yilers.upm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * @program: base
 * @description: 日志
 * @author: hui.zhang
 * @date: 2021/1/27 4:44 下午
 **/

@Data
@TableName("upm_log")
@EqualsAndHashCode(callSuper = true)
public class Log extends Model<Log> {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 模块
     */
    private String module;

    /**
     * 动作 增删改
     */
    private String action;

    /**
     * 是否成功 1-是 0-否
     */
    private Integer success;

    /**
     * 操作人
     */
    private String operator;

    /**
     * 请求方法
     */
    private String method;

    /**
     * 请求参数
     */
    private String params;

    /**
     * 请求ip
     */
    private String ip;

    private String region;

    /**
     * 耗时
     */
    private Integer duration;

    /**
     * 状态 1-正常 0-删除
     */
    @TableLogic
    private Integer deleted;

    private Long tenantId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @Schema(description = "异常堆栈")
    private String stackTrace;

    private Long deptId;

}
