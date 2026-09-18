# UPM 单点登录设计

## 目标

独立部署的 UPM 作为唯一认证中心。租户在 UPM 中维护应用、菜单、角色和用户；业务应用完成一次 SSO 后，使用自己的 Sa-Token 登录态访问自身接口。应用之间跳转时复用 UPM 的认证会话，因此用户无需再次输入账号密码。

整个流程不依赖 Cookie。UPM 管理端和各业务前端分别把自己的不透明 token 保存在各自域名的 `localStorage`，并通过 `Authorization` 请求头发送。

## 客户端标识

SSO `client` 固定为：

```text
{tenantCode}:{applicationCode}
```

应用编码在租户内唯一。客户端密钥由 UPM 生成，数据库只保存 AES-GCM 密文，管理页面只显示是否已配置；重置密钥时明文只返回一次。

## 模式

采用 Sa-Token SSO 模式三：UPM 与业务应用不共享 Redis，业务后端持有自己的 token。业务后端使用应用密钥签名并向 UPM 校验一次性 ticket，校验响应同时返回租户、应用、用户、角色、菜单按钮和数据权限上下文。

## 登录流程

1. 业务前端发现自身未登录，跳转到 UPM 前端的 `/sso/authorize`，携带 `client`、`redirect` 和随机 `state`。
2. UPM 前端没有 UPM token 时先进入原登录页，登录完成后回到授权页。
3. 授权页用请求头中的 UPM token 调用 `/sso/authorize`。
4. UPM 校验租户、应用、回调白名单和用户在该应用下的权限，再使用 Sa-Token 生成短时、一次性 ticket。
5. 浏览器回到业务应用回调页。业务前端把 ticket 交给自己的后端。
6. 业务后端使用 `client` 和密钥签名调用 UPM `/sso/pushS` 校验 ticket。
7. 校验成功后，业务后端根据返回的登录上下文签发自己的 token，前端保存后进入业务首页。

ticket 不能当作业务 token 使用，也不能由浏览器直接换取长期 token。`state` 必须由业务应用生成并校验，用于防止登录 CSRF。

## 应用配置

- 应用名称、编码、图标、Logo、首页地址。
- 是否启用 SSO。
- 精确匹配的回调地址白名单，不接受任意通配域名。
- SSO 推送地址，用于单点注销。
- 加密保存的客户端密钥及最后更新时间。

应用启用 SSO 前必须配置密钥、至少一个回调地址和推送地址。内置 `infra` 应用是认证中心自身，不作为 SSO Client 修改。

## 安全边界

- 前端永远不持有客户端密钥。
- UPM token 只用于 UPM 域名；业务 token 只用于所属业务系统。
- 平台管理员不会自动获得其他租户业务应用的访问权。
- 用户必须属于应用所在租户，并且至少拥有一个该应用、当前终端下的有效菜单或按钮权限。
- 新租户复制应用时清空全部 SSO 配置，避免共享凭据。
- 生产环境必须通过 `UPM_SSO_SECRET_KEY` 提供 32 字节 Base64 主密钥，并使用 HTTPS。

## 服务端配置

```properties
upm.sso.secret-key=${UPM_SSO_SECRET_KEY:}
upm.sso.ticket-timeout=60
```

未配置主密钥时，普通 UPM 功能仍可运行，但不能生成、解密或启用 SSO 客户端。

可以用下列命令生成主密钥，生成后放入部署平台的安全环境变量，不要提交到 Git：

```bash
openssl rand -base64 32
```

## UPM 接口

### 获取应用展示信息

```http
GET /sso/application?client=baidu.com:oa
```

该接口不需要登录，只返回租户与应用的名称、Logo、首页等公开字段，不返回密钥和内部配置。

### 创建授权 ticket

```http
POST /sso/authorize
Authorization: Bearer {upmToken}
Content-Type: application/json

{
  "client": "baidu.com:oa",
  "redirect": "https://oa.example.com/sso/callback",
  "state": "业务系统生成的一次性随机值"
}
```

成功后返回 `redirectUrl`，UPM 前端使用 `window.location.replace` 跳转。回调地址必须与应用白名单精确匹配。

### 校验 ticket

业务后端按 Sa-Token SSO Client 模式三，以 `client` 和客户端密钥签名调用：

```http
POST /sso/pushS
```

请求参数、签名、ticket 一次性消费和单点注销登记均使用 Sa-Token 原生协议。成功响应中的 `upmContext` 包含：

- `application`：租户及应用公开信息。
- `user`：用户 ID、账号、姓名、头像、部门和职位。
- `roles`：当前用户的有效角色。
- `menus`：当前应用和登录终端下的菜单、按钮。
- `permissions`：去重后的权限编码。
- `dataScope`：角色默认数据范围；具体接口的用户覆盖规则仍由业务服务执行。

## 业务应用接入约定

业务前端发起登录时跳转：

```text
https://upm.example.com/sso/authorize?client=baidu.com:oa&redirect=https%3A%2F%2Foa.example.com%2Fsso%2Fcallback&state=...
```

业务回调页必须先校验自己保存的 `state`，再把 ticket 提交到同域业务后端。业务后端校验 ticket 后签发自身 Sa-Token，并根据 `upmContext` 初始化当前会话。客户端密钥只能放在业务后端配置或密钥管理系统中。

应用间跳转时仍走相同地址。只要 UPM 前端的 `localStorage` 中登录态有效，授权页会直接下发新 ticket，不再显示登录表单。
