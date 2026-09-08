package io.github.yilers.upm.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UserDataScopeItemRequest {

    @NotBlank
    @Size(max = 100)
    private String interfacePath;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer dataScope;

    private List<@NotNull @Positive Long> deptIdList;
}
