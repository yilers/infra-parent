# 业务应用接入 UPM SSO

本指南以 OA 为例，采用 Spring Boot 4、Jackson 3 和 Sa-Token 1.45.0 的 SSO 模式三。流程已在 jzy-platform 联调：账号密码或钉钉登录 UPM，业务后端通过 HTTP 校验一次性 ticket，再签发自己的 token。

业务应用不需要引入 `upm-core`，不需要 UPM 用户表，也不需要与 UPM 共享 Redis。以下 Java 示例使用 Lombok，按文件名分别放入业务项目并补充项目包名；HTTP 路径可以按业务规范调整。

## 1. 在 UPM 配置应用

在登录用户所属租户下创建应用，例如：

| 配置 | 本地示例 | 用途 |
| --- | --- | --- |
| 租户编码 | `yilers.com` | 用户及应用所属租户 |
| 应用编码 | `oa` | 创建后不可修改 |
| 完整客户端标识 | `yilers.com:oa` | 授权和 ticket 校验使用同一标识 |
| 应用首页 | `http://127.0.0.1:9004/home.html` | 业务系统首页 |
| 授权回调地址 | `http://127.0.0.1:9004/sso-callback.html` | 浏览器接收 ticket 和 state |
| 注销推送地址 | `http://127.0.0.1:9004/sso/pushC` | UPM 后端通知业务系统注销 |

生成客户端密钥、保存配置并开启 SSO。密钥只显示一次，放在业务后端环境变量中。UPM 的 `UPM_SSO_SECRET_KEY` 是用于加密应用密钥的主密钥，与业务客户端密钥不同，不能互换。

为应用配置 Web 菜单或按钮，再分配给用户角色。当前 UPM 要求用户属于应用租户，并至少拥有该应用、当前终端下的一个有效菜单或按钮权限；平台管理员身份本身不代表具有跨租户访问权。

回调白名单必须与业务发起登录时的 `redirect` 完全一致，包括协议、主机、端口、路径。注销推送地址必须能被 UPM 后端访问，生产环境不能填仅在业务服务器本机有效的 `127.0.0.1`。

新租户复制应用后，SSO 密钥、回调、推送地址和启用状态会清空，需要为新租户重新配置。

## 2. 业务后端依赖与配置

已经使用 `common-auth` 的项目无需重复引入 Sa-Token Spring Boot starter。使用独立 Spring Boot 的项目添加对应 starter，并配置 Sa-Token 登录及注解鉴权拦截器。Redis 持久化按业务部署需要选择；多个业务服务实例须共享自身的会话存储。

```xml
<!-- 与业务项目的 Sa-Token core、starter 版本保持一致 -->
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-sso</artifactId>
    <version>1.45.0</version>
</dependency>
<!-- 仅在未引入 common-auth 或相应 starter 时添加 -->
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-spring-boot4-starter</artifactId>
    <version>1.45.0</version>
</dependency>
```

```properties
business.sso.server-url=${UPM_SSO_SERVER_URL:http://127.0.0.1:9000}
business.sso.authorize-url=${UPM_SSO_AUTHORIZE_URL:http://127.0.0.1:9000/#/sso/authorize}
business.sso.client=${UPM_SSO_CLIENT:yilers.com:oa}
business.sso.secret-key=${UPM_SSO_CLIENT_SECRET:}
business.sso.callback-url=${BUSINESS_SSO_CALLBACK_URL:http://127.0.0.1:9004/sso-callback.html}

sa-token.token-name=Authorization
sa-token.token-prefix=Bearer
sa-token.is-read-cookie=false
```

`server-url` 指向 UPM 后端；`authorize-url` 指向 UPM 前端。独立前端开发时后者可为 `http://127.0.0.1:5666/#/sso/authorize`，内嵌前端为 `http://127.0.0.1:9000/#/sso/authorize`。当前 UPM 前端使用 Hash 路由，不要遗漏 `/#/`。

```java
// BusinessSsoProperties.java
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "business.sso")
public class BusinessSsoProperties {
    private String serverUrl;
    private String authorizeUrl;
    private String client;
    private String secretKey;
    private String callbackUrl;
}
```

## 3. 配置 Sa-Token 原生客户端

由 Sa-Token 构造协议参数、签名及校验请求，不手工拼接签名。客户端模板显式持有配置，避免读取到匿名默认配置导致 `client=null`。示例将处理器作为 Spring Bean 注入，登录、退出和推送入口使用同一实例。

```java
// BusinessSsoConfig.java
import cn.dev33.satoken.sso.config.SaSsoClientConfig;
import cn.dev33.satoken.sso.processor.SaSsoClientProcessor;
import cn.dev33.satoken.sso.template.SaSsoClientTemplate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(BusinessSsoProperties.class)
public class BusinessSsoConfig {
    @Bean
    public SaSsoClientProcessor businessSsoProcessor(BusinessSsoProperties properties) {
        if (properties.getClient() == null || properties.getClient().isBlank()
                || properties.getSecretKey() == null || properties.getSecretKey().isBlank()) {
            throw new IllegalStateException("SSO客户端标识或密钥未配置");
        }
        SaSsoClientConfig config = new SaSsoClientConfig()
                .setClient(properties.getClient())
                .setServerUrl(properties.getServerUrl())
                .setPushUrl("/sso/pushS")
                .setSecretKey(properties.getSecretKey())
                .setIsHttp(true)
                .setIsSlo(true)
                .setRegLogoutCall(false)
                .setIsCheckSign(true);
        config.setMode("ticket");
        SaSsoClientTemplate template = new SaSsoClientTemplate() {
            @Override
            public SaSsoClientConfig getClientConfig() {
                return config;
            }
        };
        RestClient restClient = RestClient.create();
        template.strategy.sendRequest = url ->
                restClient.get().uri(url).retrieve().body(String.class);
        SaSsoClientProcessor processor = new SaSsoClientProcessor();
        processor.ssoClientTemplate = template;
        return processor;
    }
}
```

`isHttp=true` 才会通过 HTTP 校验 ticket；`setMode("ticket")` 仅是模式说明。UPM 使用应用表中的注销推送地址，客户端无需通过 `regLogoutCall` 注册另一个注销入口。正式部署应为 HTTP 客户端设置连接和读取超时。

## 4. 接收登录上下文并签发业务 token

以下 DTO 对应 UPM 返回的 `upmContext`。ID 使用 `Long`，允许从字符串 ID 转换。菜单保留为 Map，由业务前端适配自身菜单模型。

```java
// BusinessLoginContext.java
import java.util.List;
import java.util.Map;

public record BusinessLoginContext(
        Application application, User user, List<Role> roles,
        List<Map<String, Object>> menus, List<String> permissions, List<Long> dataScope) {
    public record Application(String clientId, Long tenantId, String tenantCode,
            String tenantName, String tenantLogo, String applicationCode,
            String applicationName, String applicationLogo, String homeUrl) {}
    public record User(Long id, String account, String nickname, String name,
            String photo, Long deptId, Long positionId) {}
    public record Role(Long id, String code, String name, Integer dataScope) {}
}
```

使用 Spring 注入的普通 Jackson 3 `ObjectMapper` 转换 HTTP 数据。不要使用 `SaManager.getSaJsonTemplate()` 转换外部 DTO：运行环境可能为其启用 Redis 多态序列化，此时普通 JSON 缺少 `@class` 会报 `InvalidTypeIdException`。

```java
// BusinessSsoService.java
import cn.dev33.satoken.sso.model.SaCheckTicketResult;
import cn.dev33.satoken.sso.processor.SaSsoClientProcessor;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class BusinessSsoService {
    private static final String CONTEXT_KEY = "business:sso-context";
    private final SaSsoClientProcessor processor;
    private final BusinessSsoProperties properties;
    private final ObjectMapper objectMapper;

    public BusinessLoginContext login(String ticket) {
        SaCheckTicketResult result = processor.checkTicket(ticket);
        Object source = result.result == null ? null : result.result.get("upmContext");
        if (source == null) {
            throw new IllegalStateException("UPM未返回登录上下文");
        }
        BusinessLoginContext context = objectMapper.convertValue(source, BusinessLoginContext.class);
        if (context.application() == null || context.user() == null
                || context.user().id() == null || context.application().tenantId() == null
                || !properties.getClient().equals(context.application().clientId())) {
            throw new IllegalStateException("UPM登录上下文不完整或客户端不匹配");
        }
        SaLoginParameter parameter = new SaLoginParameter()
                .setDeviceType("web").setDeviceId(result.deviceId);
        if (result.remainTokenTimeout != null && result.remainTokenTimeout != 0) {
            parameter.setTimeout(result.remainTokenTimeout);
        }
        StpUtil.login(context.user().id(), parameter);
        // 存为普通JSON字符串，业务读取时也使用同一个ObjectMapper。
        StpUtil.getTokenSession().set(CONTEXT_KEY, objectMapper.writeValueAsString(context));
        return context;
    }

    public BusinessLoginContext current() {
        StpUtil.checkLogin();
        Object value = StpUtil.getTokenSession().get(CONTEXT_KEY);
        if (!(value instanceof String json) || json.isBlank()) {
            throw new IllegalStateException("登录上下文已失效，请重新登录");
        }
        return objectMapper.readValue(json, BusinessLoginContext.class);
    }
}
```

Jackson 3 示例不可直接套用到 Spring Boot 3/Jackson 2 项目；后者使用对应的 `com.fasterxml.jackson.databind.ObjectMapper` 并处理受检异常。

```java
// BusinessSsoController.java
import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.sso.processor.SaSsoClientProcessor;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/sso")
@RequiredArgsConstructor
public class BusinessSsoController {
    private final BusinessSsoProperties properties;
    private final BusinessSsoService service;
    private final SaSsoClientProcessor processor;

    public record TicketRequest(@NotBlank String ticket) {}

    @SaIgnore
    @GetMapping("/config")
    public SaResult config() {
        // 不返回secretKey；前端只需要公开配置。
        return SaResult.ok().set("authorizeUrl", properties.getAuthorizeUrl())
                .set("client", properties.getClient()).set("callbackUrl", properties.getCallbackUrl());
    }

    @SaIgnore
    @PostMapping("/login")
    public SaResult login(@Valid @RequestBody TicketRequest request) {
        service.login(request.ticket());
        return SaResult.ok().set("tokenName", StpUtil.getTokenName())
                .set("tokenValue", StpUtil.getTokenValue());
    }

    @GetMapping("/current")
    public BusinessLoginContext current() {
        return service.current();
    }

    @SaIgnore
    @RequestMapping("/pushC")
    public Object pushClientMessage() {
        // Sa-Token验证UPM通知签名，再注销本地会话。
        return processor.ssoPushC();
    }

    @PostMapping("/logout")
    public Object logout() {
        StpUtil.checkLogin();
        try {
            return processor.ssoLogout();
        } catch (RuntimeException exception) {
            log.warn("通知UPM单点退出失败，执行本地退出", exception);
            StpUtil.logout();
            return SaResult.ok();
        }
    }
}
```

示例响应使用 SaResult（`code` 和字段位于顶层）；使用 infra 的 `Result` 时字段位于 `data`，前端须对应调整。异常交给业务项目统一异常处理器转成失败响应。

放行 `/sso/config`、`/sso/login`、`/sso/pushC`、登录页和回调页。`@SaIgnore` 是否足够取决于现有拦截器；存在统一 URL 登录检查时，还需配置对应排除路径。`/sso/current`、`/sso/logout` 和业务接口必须检查本地登录态。

## 5. 浏览器发起登录与回调

将下面两个脚本分别放入业务登录页和 `sso-callback.html`。浏览器使用同一标签页跳转；发起登录和回调必须属于同一个 origin（协议、主机、端口均相同），否则读不到 `sessionStorage` 中的 state。

```javascript
// 登录页：绑定到“统一登录”按钮。
async function startLogin() {
  const response = await fetch('/sso/config');
  const config = await response.json();
  if (!response.ok || config.code !== 200) throw new Error('读取SSO配置失败');
  const state = crypto.randomUUID().replaceAll('-', '');
  sessionStorage.setItem('business:sso-state', state);
  const target = new URL(config.authorizeUrl);
  // Hash路由的参数写在#/sso/authorize之后。
  const route = target.hash.slice(1).split('?')[0];
  const params = new URLSearchParams({
    client: config.client, redirect: config.callbackUrl, state,
  });
  target.hash = `${route}?${params}`;
  location.assign(target.toString());
}
```

```javascript
// sso-callback.html：页面加载时执行，并向用户展示错误和重新登录入口。
async function finishLogin() {
  const params = new URLSearchParams(location.search);
  const ticket = params.get('ticket');
  const state = params.get('state');
  const expected = sessionStorage.getItem('business:sso-state');
  if (!ticket || !state || !expected || state !== expected) {
    throw new Error('登录回调校验失败，请重新登录');
  }
  sessionStorage.removeItem('business:sso-state');
  // 校验后清掉地址中的ticket，避免刷新重复提交。
  history.replaceState(null, '', location.pathname);
  const response = await fetch('/sso/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ ticket }),
  });
  const result = await response.json();
  if (!response.ok || result.code !== 200 || !result.tokenValue) {
    throw new Error(result.msg || '登录失败，请重新发起登录');
  }
  localStorage.setItem('business:token', result.tokenValue);
  location.replace('/home.html');
}
```

浏览器密码登录和钉钉登录都由 UPM 页面处理，业务应用不需要再接入钉钉 SDK。业务前端仅在请求自身后端时发送 `Authorization: Bearer {businessToken}`。跨应用跳转时发起目标应用的 SSO 流程；有效的 UPM 登录态会让用户免输密码，各应用仍持有各自 token。

退出时先携带本地 token 调用 `POST /sso/logout`，随后清理本地 token 并返回登录页。收到其他应用发起的单点退出后，本地 token 会失效；业务前端应统一处理未登录响应，清理存储并返回登录页。

## 6. 接入权限和租户上下文

`/sso/current` 返回的 `permissions` 用于业务页面展示，也应由业务后端用于权限检查。独立 Sa-Token 项目可实现 `StpInterface`；使用 `common-auth` 的项目实现其 `AuthService`，从当前 token 的上下文读取角色编码和权限编码，再使用 `@SaCheckPermission("oa:document:add")` 等注解。须核对传入的 loginId 与上下文用户 ID 一致。

例如，独立 Sa-Token 项目可以提供以下适配器（使用 `common-auth` 时不要重复注册）：

```java
// BusinessPermissionService.java
import cn.dev33.satoken.stp.StpInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BusinessPermissionService implements StpInterface {
    private final BusinessSsoService service;

    private BusinessLoginContext context(Object loginId) {
        BusinessLoginContext context = service.current();
        if (!context.user().id().toString().equals(String.valueOf(loginId))) {
            throw new IllegalStateException("当前用户与登录上下文不一致");
        }
        return context;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        List<String> permissions = context(loginId).permissions();
        return permissions == null ? List.of() : permissions;
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        List<BusinessLoginContext.Role> roles = context(loginId).roles();
        return roles == null ? List.of() : roles.stream().map(BusinessLoginContext.Role::code).toList();
    }
}
```

使用 `common-web` 时，在认证成功的业务请求中，将 `application.tenantId`、`user.id` 恢复到 `RequestContextHolder`。独立项目则使用自身请求上下文。租户插件只能从可信服务端会话取 ID，不能读取前端自定义租户请求头。自行使用 ThreadLocal 时须在请求完成后清理；异步任务需要显式传递上下文。

当前 `dataScope` 是角色默认部门范围：`null` 表示全部，`[-1]` 表示无部门范围，其他值为允许的部门 ID。SSO 只传递此信息，不会自动给业务 SQL 增加过滤条件；部门、仅自己及具体接口的用户覆盖规则需要业务服务适配自身表结构。`menus` 也不会自动生成业务路由，业务前端需提供对应页面和组件。

角色、菜单和权限保存在业务登录上下文中，是登录时的快照。UPM 修改权限后，已有业务会话不会自动同步；重新登录可获取新上下文。需要实时撤权时，应另行实现会话注销或上下文刷新机制。

## 7. 常见联调问题

| 现象 | 检查与处理 |
| --- | --- |
| `该 ticket 不属于 client=null` | 校验请求未带正确客户端标识。核对完整 `租户编码:应用编码`，并让客户端模板显式持有配置。 |
| `InvalidTypeIdException`，缺少 `@class` | 外部普通 JSON 被交给多态序列化模板。使用 Spring 普通 ObjectMapper 的 `convertValue`。不要要求 UPM 返回 Java 类名。 |
| 账号密码正常，钉钉后进入 UPM 首页 | UPM 第三方登录回调丢失原 SSO 授权目标。升级到包含修复的前端；静态 `third-auth-callback.html` 和登录回调处理都要恢复目标。 |
| 源码改了，9000内嵌页面仍是旧行为 | 独立前端热更新不会更新后端资源。生产模式打包（API地址为 `/`），替换 `upm-core/src/main/resources/static`，重新构建或同步运行时资源并重启后端。 |
| 无效或过期 ticket | ticket 默认有效期60秒且只消费一次，从业务登录页重新发起，不能重复刷新原回调。 |
| 回调 state 校验失败 | 核对发起页面与回调的协议、主机、端口和标签页；不要混用localhost、127.0.0.1及局域网IP。 |
| 用户没有应用访问权限 | 核对用户所属租户、应用启用状态，以及角色是否有该应用Web终端下的有效菜单或按钮。 |
| 签名校验失败 | 核对业务后端密钥是否为该租户该应用最新生成的客户端密钥；重置后旧密钥立即失效。 |
| 注销后另一个页面仍显示已登录 | 浏览器保留token并不代表服务端会话有效，下一次请求应处理未登录响应；同时检查UPM能否访问推送地址。 |

验证顺序：账号密码登录 → `/sso/current` → 权限接口 → 退出 → 钉钉登录 → UPM已登录时免密进入 → 从UPM退出后业务token失效。生产部署使用 HTTPS，不记录 ticket、业务 token 和客户端密钥，不将密钥提交到仓库。

认证中心的主密钥、应用隔离规则和协议详情见 [UPM SSO设计](SSO.md)。
