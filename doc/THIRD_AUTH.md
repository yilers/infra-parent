# UPM 第三方认证设计

## 目标与边界

第三方认证用于让 UPM 自身账号绑定钉钉等外部身份，并在后续支持扫码或授权登录。第三方认证与 SSO 的职责不同：

- 第三方认证解决“用户如何登录 UPM”。
- SSO 解决“用户登录 UPM 后，如何免登录进入业务应用”。

当前阶段只实现第三方认证平台的配置管理，不包含钉钉授权地址、授权回调、账号绑定、扫码登录和组织架构同步。这样可以先稳定配置模型和租户边界，再逐步接入具体平台。

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
| `redirect_uri` | 平台授权完成后回到 UPM 的地址 |
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

## 租户初始化

创建新租户时，会从默认租户复制已经存在的第三方平台配置模板，但会清空：

- Client ID
- Client Secret
- 回调地址
- 授权范围

复制后的配置默认停用。平台类型和说明可以保留，避免管理员重复创建模板，同时不会把默认租户凭据泄露给新租户。

对于已有数据库，增量 SQL 只创建表、菜单和角色权限，不主动创建钉钉配置记录。管理员首次进入页面后自行新增配置即可。

## 后续接入顺序

1. 实现钉钉 Provider，封装授权地址、临时授权码换取身份等平台差异。
2. 增加短时 `state`，使用 Redis 保存并一次性消费，防止登录 CSRF。
3. 增加用户第三方账号绑定接口，复用现有 `upm_user_third` 保存绑定关系。
4. 在个人中心提供扫码绑定、解绑入口。
5. 在登录页提供钉钉登录入口；未绑定账号时禁止自动创建高权限用户。
6. 如确有需要，再单独设计部门和用户同步任务，不能把组织同步混入登录回调。

第三方登录成功后仍由 UPM 签发原有登录 token，现有角色、菜单、数据权限和 SSO 流程均保持不变。

## 数据库脚本

- 新库：直接执行 `sql/full/mysql.sql` 或 `sql/full/postgres.sql`。
- 已有 MySQL：执行 `sql/migration/20260923_third_auth.mysql.sql`。
- 已有 PostgreSQL：执行 `sql/migration/20260923_third_auth.postgres.sql`。

执行增量脚本前应停止写入并备份数据库。脚本会在当前最大菜单 ID 后，为每个租户连续创建第三方认证菜单及四个按钮权限。
