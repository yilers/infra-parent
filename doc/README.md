# 项目文档

文档按模块归属组织，模块自己的设计、使用、接入与后续规划放在同一目录；“已实现／规划中”在内容里明确标注，不再通过不同目录区分。公共技术能力说明保留在本目录。

## 业务模块

| 目录 | 内容 | 当前状态 |
| --- | --- | --- |
| [upm](upm/README.md) | 租户、应用、用户、组织、RBAC、认证与权限 | 已实现 |
| [flow](flow/README.md) | 流程设计、审批中心、动态办理人与请假示例 | 通用底座与示例已实现 |
| [shortlink](shortlink/README.md) | 多租户短链、号段、有效期与系统调用 | 已实现 |
| [notice](notice/README.md) | 站内消息、SMTP、邮件模板与持久化任务 | 1.2.1开发中；短信、钉钉、飞书待扩展 |
| [file](file/README.md) | 文件上传、下载与存储能力设计 | 规划中 |

### UPM专题

- [SSO认证中心](upm/SSO.md)
- [SSO客户端接入](upm/SSO_CLIENT.md)
- [第三方登录与绑定](upm/THIRD_AUTH.md)
- [租户应用菜单同步](upm/TENANT_PERMISSION_SYNC.md)
- [字段权限](upm/FIELD_PERMISSION.md)

### Flow专题

- [动态办理人](flow/flow-assignees.md)
- [请假业务示例](flow/flow-leave.md)

## 公共能力

- [分布式锁与限流](DISTRIBUTED_CONTROL.md)
- [后端消息国际化](backend-i18n.md)
- [数据库脚本执行说明](../sql/README.md)
- [已有库增量迁移](../sql/migration/README.md)

## 模块边界

各能力采用 `api + core` 分层，统一部署由 `infra-start` 组装，需要独立部署时再增加各自的 `start` 模块与远程适配。

`common` 只承载通用技术能力，不放置具体业务模块的实体、服务和控制器。`api` 模块保存稳定的服务契约及请求响应对象，`core` 模块保存业务实现、持久化和 HTTP 接口。

## 部署方式

模块支持两种接入方式：

1. 模块集成：业务启动器直接依赖对应的 `core`，使用本地 Service 和同一数据库。
2. 独立部署：新增对应的 `start`，业务系统只依赖 `api`，通过 HTTP 或 RPC 调用。

`infra-start` 是基础能力统一部署入口，依赖 UPM、文件、通知、工作流和短链的 `core`。`upm-start` 继续作为 UPM 独立部署入口。当前不创建空的独立启动器，出现真实的独立部署需求时再创建启动器及远程适配器。

## 依赖约束

- `file-core`、`notice-core`、`flow-core`、`shortlink-core` 不直接相互依赖。
- 上述业务 `core` 不直接依赖 `upm-core`，认证基础能力来自 `common-auth`。
- 需要用户、角色或部门信息时，通过模块自定义的扩展接口适配本地 UPM；独立部署时由组装层依赖 `upm-api` 实现远程适配。
- 第三方框架类型不得暴露到各模块的 `api` 中。
- HTTP 文件流不通过 Dubbo 等 RPC 接口传输，RPC 只传递文件标识和元数据。

## 数据规范

- 文件服务表使用 `file_` 前缀。
- 通知服务表使用 `notice_` 前缀。
- 工作流原生表使用 `flow_` 前缀，自定义扩展表使用 `flow_ext_` 前缀。
- 短链表使用 `shortlink_` 前缀；UPM 管理的通用系统调用凭证使用 `upm_api_credential` 前缀。
- 需要隔离的数据必须包含 `tenant_id`；是否关联应用按模块实际业务决定，不强制所有表增加应用字段。
- 用户、租户和应用上下文由服务端登录态恢复，不信任客户端直接提交的身份请求头。

## 后续规划

- 文件能力按[file规划](file/README.md)建设。
- Notice后续再接入短信、钉钉、飞书及外部系统HTTP发送。
- 各模块的独立启动器、服务间认证和HTTP/RPC适配，在有实际独立部署需求后补齐；现有API契约不代表远程入口已经全部实现。

## 文档维护约定

- 每个模块目录用 `README.md` 作为入口，专题文档放在同目录。
- 同一功能只维护一份正文，总索引和项目README通过链接引用，不同时保留“规划版”和“实现版”。
- 新增、移动文档时同步更新项目README、SQL迁移说明和相关文档链接。
- 功能状态、已支持范围和待实现能力分别写清楚，避免把规划描述成已经可用。
