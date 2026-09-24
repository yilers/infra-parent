# UPM 第三方认证设计

## 目标与边界

第三方认证用于让 UPM 自身账号绑定钉钉等外部身份，并在后续支持扫码或授权登录。第三方认证与 SSO 的职责不同：

- 第三方认证解决“用户如何登录 UPM”。
- SSO 解决“用户登录 UPM 后，如何免登录进入业务应用”。

当前已经实现第三方认证平台配置管理，以及已登录用户绑定、解绑钉钉账号。尚未开放钉钉直接登录、自动创建用户和组织架构同步。

## 模块位置

第三方认证属于用户身份体系，因此放在 UPM 内部：

- `upm-api`：配置实体、请求响应对象、平台枚举和服务接口。
- `upm-core`：配置管理业务、Controller、Mapper 和后续的平台 Provider。
- `common-core`：不放第三方认证业务代码，继续只提供通用工具能力。

当前不单独创建第三方认证模块。只有未来外部平台数量、协议实现和发布节奏明显独立于 UPM 时，再考虑拆分。

## 数据模型

表名为 `upm_third_auth_config`，每个租户、每个平台只能保留一条有效配置。首个平台为钉钉，平台编码固定为 `dingTalk`。

主要字段如下：

| 字段 | 含义 |
| --- | --- |
| `tenant_id` | 所属租户，由服务端请求上下文确定 |
| `platform` | 第三方认证平台编码 |
| `client_id` | 平台 AppKey 或 Client ID |
| `client_secret` | 平台 AppSecret 或 Client Secret |
| `redirect_uri` | 平台授权完成后回到 UPM 个人中心的地址 |
| `scopes` | 逗号分隔的授权范围 |
| `operable` | 是否允许管理员修改 |
| `usable` | 是否启用 |
| `version` | 乐观锁版本号 |

本阶段允许 `client_secret` 以明文落库，但它受到以下边界约束：

- 查询接口和响应对象永远不返回原文，只返回 `secretConfigured`。
- 请求对象将该字段标记为只写，Swagger 不把它展示为响应字段。
- 操作日志隐藏该字段。
- `infra-start` 和 `upm-start` 单独关闭该 Mapper 的 DEBUG SQL 参数日志。
- 编辑时留空表示保留原值，不需要把原文回显到浏览器。

业务系统直接集成 `upm-core` 时，如果自行把 Mapper 日志开启为 DEBUG，也必须增加以下配置，避免密钥进入日志文件：

```properties
logging.level.io.github.yilers.upm.mapper.ThirdAuthConfigMapper=info
```

正式开放第三方登录前，建议再增加独立的密钥加密组件；加密实现不能复用业务应用的 SSO 客户端密钥作为固定明文密钥。

## 管理接口

接口统一位于 `/thirdAuthConfig`：

| 接口 | 权限编码 | 说明 |
| --- | --- | --- |
| `GET /findAll` | `system:thirdAuth:list` | 查询当前租户配置，不返回密钥原文 |
| `POST /save` | `system:thirdAuth:add` | 新增配置，新增后默认停用 |
| `POST /update` | `system:thirdAuth:edit` | 修改配置，平台不可修改，密钥可留空 |
| `POST /usable` | `system:thirdAuth:usable` | 启停配置，启用前检查必要参数 |

管理接口不接收租户 ID，全部通过服务端登录上下文和 MyBatis Plus 租户插件隔离数据。

## 账号绑定接口

账号绑定接口统一位于 `/thirdAuth/binding`，只要求用户已经登录 UPM，不需要菜单权限：

| 接口 | 说明 |
| --- | --- |
| `GET /findAll` | 查询当前用户各平台绑定状态 |
| `POST /authorize` | 生成五分钟有效的一次性 state 和钉钉 OAuth2 授权地址 |
| `POST /bind` | 使用回调返回的授权码和 state 完成绑定 |
| `POST /unbind` | 物理删除当前用户的平台绑定，允许以后重新绑定 |

绑定流程如下：

1. 已登录用户在个人中心点击“立即绑定”。
2. UPM 把用户 ID、租户 ID 和平台编码写入 Redis 一次性 state，返回钉钉授权地址。
3. 钉钉授权后返回配置的个人中心地址，并附带 `authCode` 和 `state`。
4. 前端携带原 UPM token 调用绑定接口。
5. 后端一次性消费 state，校验它与当前登录用户、租户一致。
6. 后端使用授权码换取钉钉用户 token 并读取当前钉钉用户信息。
7. 只保存 OpenId、UnionId 和公开资料快照，不保存钉钉 access token 或 refresh token。

推荐回调地址：

```text
https://upm.example.com/third-auth-callback.html
```

该地址必须与钉钉开发者后台登记的回调地址一致。回调页会保留钉钉返回的授权参数，再转到 Hash 路由下的个人中心完成绑定。
本地开发需要使用钉钉可以访问的公网测试域名，不能直接使用仅本机可见的地址。

## 租户初始化

创建新租户时，会从默认租户复制已经存在的第三方平台配置模板，但会清空：

- Client ID
- Client Secret
- 回调地址
- 授权范围

复制后的配置默认停用。平台类型和说明可以保留，避免管理员重复创建模板，同时不会把默认租户凭据泄露给新租户。

对于已有数据库，增量 SQL 只创建表、菜单和角色权限，不主动创建钉钉配置记录。管理员首次进入页面后自行新增配置即可。

## 后续接入顺序

1. 使用测试组织的企业内部应用完成钉钉账号绑定联调。
2. 在登录页提供钉钉登录入口；未绑定账号时禁止自动创建用户。
3. 对直接登录流程增加独立的一次性 state 和登录结果交换凭证。
4. 如确有需要，再单独设计部门和用户同步任务，不能把组织同步混入登录回调。

第三方登录成功后仍由 UPM 签发原有登录 token，现有角色、菜单、数据权限和 SSO 流程均保持不变。

## 数据库脚本

- 新库：直接执行 `sql/full/mysql.sql` 或 `sql/full/postgres.sql`。
- 已有 MySQL：执行 `sql/migration/20260923_third_auth.mysql.sql`。
- 已有 PostgreSQL：执行 `sql/migration/20260923_third_auth.postgres.sql`。
- 已执行上述配置迁移的数据库，再执行对应的 `20260924_third_auth_binding` 脚本。

执行增量脚本前应停止写入并备份数据库。`20260923` 脚本创建第三方认证配置表、菜单和四个按钮权限，
`20260924` 脚本只调整用户绑定表字段并补充唯一约束。
