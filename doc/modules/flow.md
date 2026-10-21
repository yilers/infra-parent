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
- 实例流程图通过身份适配器解析本租户的审批人名称，并以只读 `permissionLabel` 展示；原始办理人标识、监听器和变量仍不返回。未配置或无法解析的审批人明确提示，不显示成“所有人”，也不要求普通审批人具备流程设计权限。
- 支持申请人、本部门副主管优先（无则主管）、上一级部门主管等动态办理人规则，使用Warm-Flow原生变量表达式。配置方法、安全边界和请假业务边界见[动态办理人配置](flow-assignees.md)。
- 租户、应用和业务数据关联。
- 用户、角色和部门审批人解析。
- 审批节点通过后自动抄送：在节点“通知抄送”页签选择用户、角色、部门或动态规则，接收人在“抄送我的”中查看并标记已读。

OA 首期作为 `infra` 应用内与“系统管理”同级的菜单，无需独立 SSO 或第二套前端。流程设计使用 Warm-Flow 官方 Vue 组件；待办、已办、我发起的等页面由 `infra-parent-web` 实现。

## Warm-Flow选型

选用 Warm-Flow 1.8.9 作为底层流程引擎，`flow-core` 引入官方 `warm-flow-mybatis-plus-sb4-starter`。已通过 JDK 25 的 Maven 编译；实际启动、建表和流程流转仍需在目标数据库上验证。1.8.9 的审批通过和驳回使用 `TaskService.pass/reject`，不再以旧的 `InsService.skipByInsId` 作为新接入的默认入口。

Warm-Flow 类型只允许出现在 `flow-core`，不得暴露到 `flow-api`。业务模块通过项目自己的 `WorkflowService` 操作流程，避免底层引擎升级影响业务契约。

状态判断在后端复用Warm-Flow枚举，在前端由 `api/flow/constants.ts` 维护对应协议编码，不在页面散落数字比较。发布状态包含“已失效”，不等同于未发布草稿；已有实例的定义版本只能复制后修改。UPM主管类型由 `DeptLeaderTypeEnum` 定义，请假类型及校验边界分别由 `LeaveTypeEnum`、`LeaveConst` 定义，不改变现有编码。

当前任务审批、退回和业务重提已经通过实例行锁处理重复办理；流程定义的设计、复制、发布属于管理操作，目前应按流程编码串行维护。同一编码的并发发布、设计保存与发布之间的互斥尚需专项补齐，不能把实例锁等同于定义管理的并发保护。

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

`WorkflowIdentityProvider.resolveUsers` 按用户ID批量返回最小展示信息。待办、已办、详情中的发起人和历史办理人姓名使用该接口填充，查不到时保留原始ID；抄送创建时复用它固化接收人姓名。历史办理人姓名展示的是当前用户资料，不回写流程历史表；抄送接收人姓名是触发时的快照。

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

审批中心已接入“我的待办 / 我的已办 / 我发起的 / 抄送我的”及实例详情、审批记录和只读运行图。发起人、历史办理人、当前候选人或实际抄送接收人可查看实例详情；管理员没有默认旁路。待办使用 `flow_user` 权限交集及 EXISTS 查询，避免同时命中用户、角色、部门时重复显示。未配置办理人的任务不会向全租户开放。挂起实例和流程不列入可办理任务。

接口为 `GET /flow/inbox/todo`、`GET /flow/inbox/done`、`GET /flow/inbox/started` 和 `GET /flow/inbox/detail/{id}`，分别要求 `flow:task:todo`、`flow:task:done`、`flow:instance:mine`；详情满足任意一种菜单查询权限后仍须通过实例参与人授权。分页支持 `flowCode` 精确匹配，`pageNum >= 1`，`1 <= pageSize <= 100`。已办按本人实际办理的审批节点记录展示，不将发起记录混入已办。

列表与详情不返回原始流程变量、业务扩展正文、监听器类路径和表单路径。只读运行图的节点属性面板会显示办理人名称、监听器事件摘要及抄送对象名称；角色、部门和动态规则均解析为展示名称，不暴露原始配置。运行图保留引擎的节点/连线状态，不使用流程定义替代实例快照。业务单据ID只用于关联，不代表获取业务正文的授权；业务详情入口与业务鉴权仍由后续业务模块实现。

已有库执行 `20261019_flow_inbox` 对应数据库脚本一次：在模板租户新增三个菜单、三个查询按钮及查询索引，停写期间从现有最大菜单ID后连续分配ID。其他租户同步应用菜单后授权，重新登录刷新菜单。新库已包含在全量脚本内，不再执行增量。

审批操作已开放 `POST /flow/task/pass/{id}` 和 `POST /flow/task/reject/{id}`，对应 `flow:task:pass`、`flow:task:reject`。请求只接收 `{ "message": "审批意见" }`，最多500个字符，退回原因必填；客户端不能指定办理人、租户、跳转目标或覆盖流程变量。退回遵循流程设计的 REJECT 路线，不代表终止；未配置合法退回路线时由引擎拒绝并回滚。

引擎不允许退回开始节点。需要申请人修改后重新提交时，应设计“开始 → 申请提交（普通节点，申请人办理）→ 审批 → 结束”，审批的退回路线指向“申请提交”，而不是开始节点。请假示例已实现首次提交自动完成唯一申请节点、退回后修改及原实例重提，配置见[请假业务说明](flow-leave.md)。

`WorkflowBusinessProvider.findSubmissionTask` 由业务识别当前申请任务，向审批中心提供业务入口。通用通过、退回接口拒绝办理这类任务，避免绕过表单校验；业务Service完成授权、校验和保存后，在同一READ_COMMITTED事务中调用 `WorkflowService.submitBusinessTask`。该方法校验业务类型及候选身份，仅作为Service契约，不提供通用HTTP接口。通用引擎不会仅因节点办理人是申请人就自动完成任务。

`WorkflowService.start` 表示发起审批，初始实例状态设置为审批中，而不是引擎默认的待提交。服务通过实例行锁串行化同一实例的办理，再检查当前任务及实际候选身份；审批方法使用 READ_COMMITTED 隔离，避免等待行锁后继续读取旧快照。直接注入Service的业务模块若在外层开启事务，必须同样采用 READ_COMMITTED；不要在已有 REPEATABLE_READ 事务中调用审批方法。所有流程写入和业务状态更新必须共用同一数据源及事务管理器。重复提交已完成任务会被拒绝，不再推进第二次。

UPM 适配器在任务创建时把用户、角色、部门转换为本租户的有效用户ID，候选人随任务固化；角色/部门人员变化不自动改写已生成任务，后续可通过转办等功能调整。部门表示本部门员工，不含下级部门，也不表示部门主管。只依赖Flow的其他宿主可实现 `WorkflowIdentityProvider.resolveUserIds`，默认只支持用户标识。没有有效办理人或保留未解析的类型标识时整笔事务回滚，避免生成无人可办任务。

已有库继续执行 `20261019_flow_task_actions` 对应脚本一次，为待办菜单新增两个按钮；其他租户同步菜单并授权后重新登录。请假示例现已接入，见[请假业务说明](flow-leave.md)，新增 `flow_ext_leave` 业务表及独立的业务包，通过 `WorkflowBusinessProvider` 提供关联。动态部门主管规则和站内抄送已接入；钉钉、邮件等外部通知渠道尚未接入。

### 节点自动抄送

审批节点的“通知抄送”页签保存到 Warm-Flow 节点 `ext` 中的 `flowCc` 扩展值，不覆盖其他扩展字段。设计器对扩展字段的逗号有特殊处理，因此值使用 URI 编码的 JSON。启用后至少选择一个对象，最多100项；支持用户、角色、部门和申请人、本部门副主管优先、上一级部门主管等动态规则。勾选“包含下级部门”后对本节点选中的所有部门生效。

引擎在节点真正通过时调用全局完成监听器；退回、尚未满足通过条件的会签不写抄送。接收对象在触发时按当前租户有效账号展开、去重，并把实际接收用户ID、姓名和触发节点存入 `flow_cc`。组织变化不重写历史记录；没有有效接收人的规则不阻塞已经通过的审批。抄送只提供知晓和阅读状态，不产生审批任务，也不调用 Notice、钉钉或邮件。

`GET /flow/inbox/cc` 和 `POST /flow/inbox/cc/read/{id}` 使用 `flow:cc:list` 按钮权限，列表与已读操作均校验接收人和租户。抄送接收人可通过受保护详情接口查看实例和请假正文，不能因抄送而审批任务或查看其他草稿。已有库执行 `20261020_flow_cc` 对应数据库脚本一次；它新建 `flow_cc` 表、“抄送我的”菜单和查询按钮。其他租户同步应用菜单后按需授权。新库的全量脚本已包含这些结构。

只依赖 `flow-core` 的其他启动器必须实现 `WorkflowIdentityProvider`，并将 `flowTenantLineInnerInterceptor` 加入自己的 MyBatis-Plus 插件链；否则启动检查会拒绝运行。`upm-start` 不依赖 Flow，不受此配置影响。

“部门主管审批”通过 UPM 的 `upm_dept_leader` 关系解析，不能将菜单按钮权限当作某条任务的办理权限。内置规则在发起时保存审批人快照，任务到达时由引擎生成实际候选人；缺失审批人时明确报错，除已明确的副主管到本部门主管兜底之外，不自动跨部门寻找替代。请假示例可沿用固定办理人，也可配置[动态规则](flow-assignees.md)。独立部署时还需补齐可信的远程身份校验与权限查询适配，不能仅引入 `common-auth` 的默认空权限实现。

## File与Notice集成

业务附件保存在 File，流程只保存业务标识或文件标识，不复制文件内容。审批完成、驳回和待办提醒通过 Notice 发送，但 `flow-core` 不直接依赖 `file-core` 或 `notice-core`，由扩展接口或领域事件完成适配。

## 前端规划

OA 首期页面全部放在当前管理端，菜单归属现有 `infra` 应用；Flow 后端将来独立部署时，前端菜单归属不必改变。公共的流程图、审批记录和操作组件以后可以抽取为前端 npm 包，不在后端模块中耦合具体页面实现。
