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

- `flow-api`：工作流服务契约、请求响应对象和身份解析扩展接口。
- `flow-core`：流程定义、实例、任务、审批操作及工作流引擎适配。
- `flow-start`：需要独立部署时再创建。

## 功能范围

- 流程定义、版本、发布和停用。
- 发起、审批、驳回、撤回、转办和终止。
- 待办、已办、我发起的流程和流程历史。
- 流程图与当前节点状态。
- 租户、应用和业务数据关联。
- 用户、角色和部门审批人解析。

## Warm-Flow选型

计划使用 Warm-Flow 作为底层流程引擎。接入前验证其与 Spring Boot 4、JDK 25、MyBatis-Plus、租户插件和当前数据库版本的兼容性。

Warm-Flow 类型只允许出现在 `flow-core`，不得暴露到 `flow-api`。业务模块通过项目自己的 `WorkflowService` 操作流程，避免底层引擎升级影响业务契约。

## 数据规划

Warm-Flow 原生表保持其 `flow_` 命名。项目新增的应用映射、业务关联或其他扩展表统一使用：

```text
flow_ext_xxx
```

由 Warm-Flow 自己处理的表不再重复实现。MyBatis-Plus 租户插件与 Warm-Flow 租户机制只能保留一个明确入口，避免同一 SQL 被重复追加租户条件。

## UPM适配

`flow-core` 不直接依赖 `upm-core`。工作流定义自己的身份查询接口，用于解析用户、角色、部门和部门层级：

```text
WorkflowIdentityProvider
```

本地集成时由组装层调用 UPM Service 实现；独立部署时由 `flow-start` 依赖 `upm-api`，通过 RPC 或 HTTP 获取身份数据。租户、应用和操作人由服务端登录态恢复。

## File与Notice集成

业务附件保存在 File，流程只保存业务标识或文件标识，不复制文件内容。审批完成、驳回和待办提醒通过 Notice 发送，但 `flow-core` 不直接依赖 `file-core` 或 `notice-core`，由扩展接口或领域事件完成适配。

## 前端规划

流程管理页面可放在基础管理平台，OA 等业务应用保留待办、已办和业务审批页面。公共的流程图、审批记录和操作组件以后可以抽取为前端 npm 包，不在后端模块中耦合具体页面实现。
