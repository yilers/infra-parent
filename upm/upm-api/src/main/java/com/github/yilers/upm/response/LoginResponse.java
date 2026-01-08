package com.github.yilers.upm.response;

import cn.dev33.satoken.stp.SaTokenInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LoginResponse extends SaTokenInfo {

    private String name;

}
