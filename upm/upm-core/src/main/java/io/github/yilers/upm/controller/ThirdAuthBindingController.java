package io.github.yilers.upm.controller;

import io.github.yilers.core.util.Result;
import io.github.yilers.upm.handler.ThirdAuthBindingHandler;
import io.github.yilers.upm.request.ThirdAuthAuthorizeRequest;
import io.github.yilers.upm.request.ThirdAuthBindRequest;
import io.github.yilers.upm.request.ThirdAuthUnbindRequest;
import io.github.yilers.upm.response.ThirdAuthAuthorizeResponse;
import io.github.yilers.upm.response.ThirdAuthBindingResponse;
import io.github.yilers.web.log.SysLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 当前用户第三方账号绑定接口。
 *
 * <p>全部接口都要求UPM登录态，不使用菜单权限，普通用户可以在个人中心管理自己的绑定。</p>
 */
@RestController
@RequestMapping("/thirdAuth/binding")
@RequiredArgsConstructor
@Tag(name = "第三方账号绑定")
public class ThirdAuthBindingController {

    private final ThirdAuthBindingHandler thirdAuthBindingHandler;

    @GetMapping("/findAll")
    @Operation(summary = "查询当前用户第三方账号绑定状态")
    public Result<List<ThirdAuthBindingResponse>> findAll() {
        return Result.ok(thirdAuthBindingHandler.findAll());
    }

    @PostMapping("/authorize")
    @Operation(summary = "生成第三方账号绑定授权地址",
            description = "生成五分钟有效的一次性state，并返回第三方平台OAuth2授权地址。")
    public Result<ThirdAuthAuthorizeResponse> authorize(
            @Validated @RequestBody ThirdAuthAuthorizeRequest request) {
        return Result.ok(thirdAuthBindingHandler.authorize(request.getPlatform()));
    }

    @PostMapping("/bind")
    @Operation(summary = "完成第三方账号绑定",
            description = "校验当前登录用户和一次性state后，使用授权码获取第三方身份并完成绑定。")
    @SysLog(module = "第三方账号绑定模块", value = "绑定第三方账号", hideFieldList = {"authCode", "state"})
    public Result<?> bind(@Validated @RequestBody ThirdAuthBindRequest request) {
        thirdAuthBindingHandler.bind(request);
        return Result.ok();
    }

    @PostMapping("/unbind")
    @Operation(summary = "解除当前用户第三方账号绑定")
    @SysLog(module = "第三方账号绑定模块", value = "解除第三方账号绑定")
    public Result<?> unbind(@Validated @RequestBody ThirdAuthUnbindRequest request) {
        thirdAuthBindingHandler.unbind(request.getPlatform());
        return Result.ok();
    }
}
