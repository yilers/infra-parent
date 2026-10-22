# infra-parent 编码指引

本文件只记录修改本仓库时容易遗漏的约定。功能介绍、环境要求和运行命令见 [README.md](README.md)；依赖版本与 Java 编译目标以根目录 [pom.xml](pom.xml) 为准，不在这里重复维护版本号。

## 模块边界

- `common/` 提供公共 API、认证、Web、Redis 和基础工具能力。
- `upm/upm-api` 放 UPM 契约和模型；`upm/upm-core` 放业务实现、HTTP 接口、SSO Server 及内嵌前端；`upm/upm-start` 是 UPM 独立启动入口。
- `flow/flow-api` 放工作流契约；`flow/flow-core` 放流程引擎和业务无关的流程逻辑。工作流所需的用户、部门、角色数据通过 Provider 接口获取。单体适配实现位于 `infra-start`，不要让 `flow-core` 直接依赖 `upm-core`。
- `file/`、`notice/` 是预留模块，新增功能前先看 `doc/modules/` 中的规划。`ai/` 是独立的 AI 相关模块。
- `infra-start` 组合基础模块统一启动；需要只部署 UPM 时使用 `upm-start`。新增模块时检查 API、core、启动器之间的依赖方向。

前端源码在独立仓库 `infra-parent-web`，不在本仓库修改前端源码。需要更新后端内嵌页面时，先在前端仓库执行 `pnpm --filter @vben/infra-web build`，再用 `apps/infra-web/dist/` 更新 `upm/upm-core/src/main/resources/static/`，清理旧的带哈希文件。前端开发服务与后端内嵌页面使用不同的构建方式，涉及样式时检查生产构建结果。

## 数据与权限约定

- 业务表按模块使用 `upm_`、`flow_`、`file_`、`notice_` 等前缀。实体的 `deleted` 表示逻辑删除；`usable` 表示启用状态，`operable` 表示是否允许操作，不要混用。
- 多租户数据隔离由 MyBatis Plus 租户插件处理。租户上下文从服务端验证过的登录态恢复，不信任客户端提交的租户 ID。确需跨租户访问时，显式审查租户插件忽略范围和授权条件。
- 菜单按钮属于租户下的应用及终端，角色属于租户。修改平台模板菜单、应用或权限时，检查新租户复制和现有租户增量同步逻辑。
- 行数据权限涉及接口资源和 SQL 过滤：参考现有 `@DataPermissionResource`、`@DataPermission` 用法，检查角色范围、用户覆盖范围及空值语义。不要仅靠前端隐藏按钮实现授权。
- 工作流接入 UPM 数据时，优先扩展 `flow-api` 的 Provider 契约及其适配实现；流程数据组装逻辑留在 `flow-core`。

## SQL 与文档

- 空库执行 `sql/full/` 中对应数据库的全量脚本；已有库按 `sql/migration/README.md` 执行尚未应用的增量脚本。两者不要叠加执行。
- 修改表结构或初始菜单、按钮时，同时更新 MySQL、PostgreSQL 全量脚本，并提供对应的增量脚本；已有库的迁移要考虑重复执行和部分失败后的核验方式。SQL 由人工执行，服务启动不会自动迁移。
- 业务设计和接入说明放在 `doc/`；面向使用者的安装、功能和版本信息放在 `README.md`、`CHANGELOG.md`。不要在本文件写固定账号、密钥或容易过期的依赖版本。

## 修改与验证

- 沿用所在模块的包名、DTO、Service 和异常处理风格；保持代码整洁，非显而易见的业务规则写清楚中文注释，避免新增魔法值。
- 保留工作区里与任务无关的修改。提交时只暂存本次相关文件，尤其不要顺手提交本地 `application-dev.properties` 配置。
- 先做与改动范围相称的检查。测试默认跳过，需要执行时显式传入 `-DskipTests=false`；例如 `mvn -pl upm/upm-core -am test -DskipTests=false`。文档变更检查链接和命令，前端资源变更检查生产构建及内嵌资源是否一致。
