# Flow 工作流服务规划

## 命名约定

模块、包、接口路径、权限编码和表前缀统一使用 `flow`：

```text
flow-api
flow-core
io.github.yilers.flow
/api/flow/**
flow:definition:list
```

Java 业务类型使用语义明确的 `Workflow` 前缀，例如 `WorkflowService`、`WorkflowStartRequest`，避免与 `java.util.concurrent.Flow` 混淆。

## 模块结构

```text
flow
├── flow-api
└── flow-core
```

- `flow-api`：工作流服务契约和身份解析扩展接口，不暴露引擎类型。
- `flow-core`：Warm-Flow 引擎适配、租户隔离和办理人权限接入。
- `flow-start`：需要独立部署时再创建。

## 功能范围

- 流程定义、版本、发布和停用。
- 发起、审批、驳回、撤回、转办和终止。
- 待办、已办、我发起的流程和流程历史。
- 流程图与当前节点状态。
- 租户、应用和业务数据关联。
- 用户、角色和部门审批人解析。

OA 首期作为 `infra` 应用内与“系统管理”同级的菜单，无需独立 SSO 或第二套前端。流程设计使用 Warm-Flow 官方 Vue 组件；待办、已办、我发起的等页面由 `infra-parent-web` 实现。

## Warm-Flow选型

选用 Warm-Flow 1.8.9 作为底层流程引擎，`flow-core` 引入官方 `warm-flow-mybatis-plus-sb4-starter`。已通过 JDK 25 的 Maven 编译；实际启动、建表和流程流转仍需在目标数据库上验证。1.8.9 的审批通过和驳回使用 `TaskService.pass/reject`，不再以旧的 `InsService.skipByInsId` 作为新接入的默认入口。

Warm-Flow 类型只允许出现在 `flow-core`，不得暴露到 `flow-api`。业务模块通过项目自己的 `WorkflowService` 操作流程，避免底层引擎升级影响业务契约。

## 数据规划

Warm-Flow 原生表保持其 `flow_` 命名。项目新增的应用映射、业务关联或其他扩展表统一使用：

```text
flow_ext_xxx
```

原生七张流程表的结构采用 [Warm-Flow v1.8.9 官方 MySQL 脚本](https://github.com/dromara/warm-flow/blob/v1.8.9/sql/mysql/warm-flow-all.sql) 与 [PostgreSQL 脚本](https://github.com/dromara/warm-flow/blob/v1.8.9/sql/postgresql/postgresql-warm-flow-all.sql)。空库执行 `sql/full/`，已有库只执行相应的 `sql/migration/20261016_flow.*.sql`；不要在已有流程表的库中重复建表。

引擎表的 `tenant_id` 为 `VARCHAR(40)`，UPM 表的 `tenant_id` 为 `BIGINT`。UPM 租户插件忽略 `flow_` 表，`flow-core` 对 `flow_` 表单独追加字符串租户条件，避免重复注入或类型混用。运行时仍需验证流程设计、发起和任务查询全链路的租户隔离。

流程实体的租户值使用 Warm-Flow 的 `TenantHandler`；SQL 条件使用 MyBatis-Plus 的 `TenantLineInnerInterceptor`。两者共用服务端 `RequestContextHolder`，没有租户上下文时直接失败。启动时还会检查 Flow 拦截器确实加入插件链、办理人处理器确实被引擎装载。设计依据见 [官方多租户说明](https://warm-flow.cn/master/advanced/tenant.html) 和 [办理人权限处理器说明](https://warm-flow.cn/master/primary/permission_handler.html)。

## UPM适配

`flow-core` 不直接依赖 `upm-core`。工作流定义自己的当前身份接口：

```text
WorkflowIdentityProvider
```

`infra-start` 中的 `UpmWorkflowIdentityProvider` 通过 UPM Service 提供当前用户ID、角色ID和部门ID；Warm-Flow 的办理人标识采用用户ID（兼容 `user:用户ID`）、`role:角色ID`、`dept:部门ID`。未登录、租户不一致或用户停用时拒绝办理。独立部署时由宿主实现同一接口，通过可信 RPC 或 HTTP 获取身份数据，不能直接复用本地 UPM 实现。

`WorkflowService` 封装发起、通过和驳回三个原子操作。`WorkflowDefinitionService` 封装定义分页、版本查询、设计 JSON 保存、复制、发布、取消发布和删除。前者仍只提供后端服务契约；后者通过 `WorkflowDefinitionController` 开放管理接口，并使用 `flow:definition:*` 按钮权限保护。其他业务模块直接注入服务时，仍必须自行校验管理权限；流程引擎的任务办理人校验不能替代这些授权。

### 流程定义管理接口

| 接口 | 权限 | 用途 |
| --- | --- | --- |
| `GET /flow/definition/page` | `flow:definition:list` | 分页查询，可按流程编码精确筛选 |
| `GET /flow/definition/versions/{flowCode}` | `flow:definition:list` | 查询全部版本 |
| `GET /flow/definition/detail/{id}` | `flow:definition:list` | 查询版本摘要 |
| `GET /flow/definition/design/{id}` | `flow:definition:list` | 导出设计 JSON |
| `POST /flow/definition/add` | `flow:definition:add` | 新建未发布定义，Body 为设计 JSON |
| `POST /flow/definition/edit/{id}` | `flow:definition:edit` | 修改未发布且无实例的版本 |
| `POST /flow/definition/copy/{id}` | `flow:definition:add` | 复制为新版本 |
| `POST /flow/definition/publish/{id}` | `flow:definition:publish` | 发布版本 |
| `POST /flow/definition/unpublish/{id}` | `flow:definition:publish` | 无实例时取消发布 |
| `POST /flow/definition/delete/{id}` | `flow:definition:delete` | 删除未发布且无实例的版本 |

`add` 和 `edit` 的 Body 是 Warm-Flow 1.8.9 `DefJson` 格式的 JSON 对象，暂不接受自行拼装的简化字段；创建时不允许指定 ID，发布状态由服务端固定为未发布。编辑时 ID、流程编码和版本须与原版本一致。流程版本已有任何实例时，即使实例已完成，也不允许编辑或删除；需要变更请复制新版本。接口仅操作当前租户数据。

模板租户的“OA审批 / 流程定义”菜单已启用，页面组件为 `flow/definition/index`。新库全量脚本直接包含该配置；已执行早期权限增量脚本的库还需执行 `20261016_flow_definition_menu`。其他租户通过现有“同步应用菜单”获得模板权限；已同步的菜单也会在本次增量中启用。执行后重新登录，以刷新菜单和按钮缓存。

流程设计器源码固定在前端 pnpm workspace 的 `packages/warm-flow-designer`，来源与本地补丁见该目录 `UPSTREAM.md`。`infra-web` 使用官方 Ant Design Vue 适配器，通过项目 `requestClient` 实现设计器数据接口，不会访问默认的 `/warm-flow/**`。`infra-start` 的 `/flow/designer/handler-*` 接口提供当前租户的角色、部门和用户候选项，办理人标识与 `WorkflowIdentityProvider` 一致；独立部署 Flow 时需要提供等价的目录适配。

待办/已办查询、请假业务和对应前端页面仍待后续接入。本阶段只完成流程定义页面与图形化设计，不声称通用 Flow 已具备完整 OA 能力。

只依赖 `flow-core` 的其他启动器必须实现 `WorkflowIdentityProvider`，并将 `flowTenantLineInnerInterceptor` 加入自己的 MyBatis-Plus 插件链；否则启动检查会拒绝运行。`upm-start` 不依赖 Flow，不受此配置影响。

“部门主管审批”后续通过 UPM 已实现的 `upm_dept_leader` 关系解析，不能将菜单按钮权限当作某条任务的办理权限。任务到达节点时应固化实际办理人；主管缺失或重复时明确报错，不自动向上级部门回退。此解析与请假流程尚未实现。独立部署时还需补齐可信的远程身份校验与权限查询适配，不能仅引入 `common-auth` 的默认空权限实现。

## File与Notice集成

业务附件保存在 File，流程只保存业务标识或文件标识，不复制文件内容。审批完成、驳回和待办提醒通过 Notice 发送，但 `flow-core` 不直接依赖 `file-core` 或 `notice-core`，由扩展接口或领域事件完成适配。

## 前端规划

OA 首期页面全部放在当前管理端，菜单归属现有 `infra` 应用；Flow 后端将来独立部署时，前端菜单归属不必改变。公共的流程图、审批记录和操作组件以后可以抽取为前端 npm 包，不在后端模块中耦合具体页面实现。
