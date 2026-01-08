package com.github.yilers.upm.response;

import com.github.yilers.upm.entity.Log;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LogInfoResponse extends Log {

    private String operatorName;
    private String tenantName;
}
