package io.github.yilers.upm.response;

import lombok.Data;

@Data
public class LoginResponse {
    public String tokenName;
    public String tokenValue;
    public Boolean isLogin;
    public Object loginId;
    public String loginType;
    public long tokenTimeout;
    public long sessionTimeout;
    public long tokenSessionTimeout;
    public long tokenActiveTimeout;
    public String loginDeviceType;
    public String tag;

    private String name;

}
