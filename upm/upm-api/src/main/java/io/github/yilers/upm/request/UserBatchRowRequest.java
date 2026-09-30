package io.github.yilers.upm.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "批量新增用户的单行数据")
public class UserBatchRowRequest {

    @Schema(description = "前端行标识，用于将校验结果对应到原始行", example = "row-1")
    private String rowKey;

    @Schema(description = "用户账号", example = "zhangsan@yilers.com")
    private String account;

    @Schema(description = "姓名", example = "张三")
    private String name;

    @Schema(description = "昵称，为空时自动使用姓名", example = "小张")
    private String nickname;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "职位ID")
    private Long positionId;

    @Schema(description = "角色ID列表")
    private List<Long> roleIdList;

    @Schema(description = "手机号码")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "性别 1-男 0-女")
    private Integer gender;
}
