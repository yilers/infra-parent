# UPM 用户权限中心

UPM 是 `infra-parent` 中的用户与权限管理模块，提供租户、应用、用户、部门、职位、角色、菜单按钮和数据权限等基础能力。

## 部署方式

UPM 支持两种互斥的接入方式：

1. 独立部署：运行 `upm-start`，业务系统通过 HTTP 或 SSO 使用统一权限中心。
2. 模块集成：业务系统依赖 `upm-api`、`upm-core`，并在自己的数据库中部署 UPM 表。此方式直接使用本地登录，不再连接另一套独立 UPM 做 SSO。

## 权限模型

数据按租户隔离。一个租户可以维护多个应用，应用下再按终端维护目录、菜单和按钮。角色属于租户，可同时勾选多个应用中的权限，用户通过角色获得权限。

客户端不得提交可信的租户 ID 或应用 ID。普通请求的用户和租户身份从 Sa-Token 登录态恢复；当前 UPM 管理端使用的应用由服务端 `upm.application-code` 配置确定。

数据权限支持全部、本部门及以下、本部门、自定义部门和仅本人。角色提供默认数据范围，用户数据权限配置用于业务上的显式覆盖。

## 新租户初始化

新租户以默认租户为模板复制应用、菜单、角色与授权关系，再创建平台管理员和租户管理员。应用的 SSO 密钥、回调地址、推送地址和启用状态不会复制，新租户必须自行完成客户端配置。

## 本地运行

```bash
mvn clean install -Drevision=1.1.0
cd upm/upm-start
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

完整数据库使用 `sql/full`，已有环境按时间顺序执行 `sql/migration`。本地连接信息只放在环境配置或环境变量中，不提交真实密码和密钥。

## 主要模块

- `common-auth`：Sa-Token 登录与权限适配。
- `common-web`：Web、MyBatis Plus、请求上下文和数据权限。
- `upm-api`：实体、请求响应对象和服务接口。
- `upm-core`：UPM 业务实现、控制器和 SSO Server。
- `upm-start`：独立部署入口。

具体 SSO 接入约定见 [SSO.md](./SSO.md)。

第三方登录平台的配置模型和后续接入顺序见 [THIRD_AUTH.md](./THIRD_AUTH.md)。
