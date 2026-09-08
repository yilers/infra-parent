package io.github.yilers.upm.request;

import io.github.yilers.api.validated.Add;
import io.github.yilers.api.validated.Update;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ApplicationRequest {
    @NotNull(groups = Update.class, message = "应用id不能为空")
    private Long id;

    @NotBlank(message = "应用名称不能为空")
    @Size(max = 100)
    private String name;

    @NotBlank(groups = Add.class, message = "应用编码不能为空")
    @Pattern(regexp = "[a-z][a-z0-9_-]{0,63}", message = "应用编码须以小写字母开头，仅支持小写字母、数字、下划线和短横线")
    private String code;

    @Size(max = 255)
    private String icon;

    @Size(max = 500)
    private String description;

    @NotNull
    @Min(0)
    private Integer sortNumber = 0;

    @NotNull(groups = Update.class, message = "版本号不能为空")
    private Integer version;
}
