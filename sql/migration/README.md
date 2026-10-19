# 应用及 SSO 改造迁移

本目录脚本需人工执行，不会随服务启动自动执行。已有数据库不要重新运行初始化脚本。

## 执行顺序

1. 停止后端写入，备份数据库；先在备份副本验证。
2. 确认租户 1 存在，菜单的 `tenant_id` 非空且对应有效的租户记录，未删除菜单的父级存在且属于同租户、同设备端。修复孤儿菜单后再迁移。
3. 确认尚无 `upm_application` 表和 `upm_permission.app_id` 列；菜单中没有 `system/application/index` 组件或 `system:application:*` 权限编码。每租户现有应用管理路由不得与新建的 `application` 路由冲突。
4. 选择数据库对应的 `20260907_application.mysql.sql` 或 `20260907_application.postgres.sql`，完整执行一次。MySQL DDL 会隐式提交，失败不能依赖事务回滚；恢复备份后排查，不要盲目重跑。
5. 部署前后端，重启后端并重新登录，刷新动态菜单。
6. 已完成应用改造的数据库继续执行对应的 `20260918_sso` 脚本，为应用表增加 SSO 配置字段。该脚本不会自动启用任何存量应用，也不会生成客户端密钥。
7. 已完成SSO改造的数据库继续执行对应的 `20260923_third_auth` 脚本，创建第三方认证平台配置表，并为每个租户增加配置菜单及按钮。该脚本不会生成任何第三方平台配置或凭据。
8. 继续执行对应的 `20260924_third_auth_binding` 脚本，扩展第三方用户标识长度并增加账号绑定唯一约束。执行前必须确认脚本顶部三组重复检查都没有返回结果。
9. 继续执行对应的 `20260925_tenant_permission_sync` 脚本，为菜单增加模板来源字段，并为平台管理员增加租户应用菜单同步权限。历史测试租户不会自动建立来源关系，同步接口会拒绝处理，应删除后使用新流程重新创建。
10. 继续执行对应的 `20260929_tenant_login_policy` 脚本，创建租户账号密码登录策略表，并为存量有效租户初始化“10分钟内失败3次、锁定10分钟”的默认策略。策略使用物理删除，删除记录后对应租户不限制密码失败次数。
11. 继续执行对应的 `20260929_user_login_unlock` 脚本，为平台模板增加“解除登录锁定”按钮权限。其他租户在租户管理中同步应用菜单后，再由租户管理员按需授权角色。
12. 已执行用户批量新增迁移的数据库，继续执行对应的 `20261014_dept_leader` 脚本，为已有 `upm_dept_leader` 表补索引并增加“设置主管”按钮。执行前确认按钮 ID `196`、权限编码 `system:dept:leader:edit` 和两个索引均不存在；成功后不要重复执行。其他租户通过“同步应用菜单”获得按钮，再按需授权角色。
13. 接入 Flow 时，先确认数据库没有 `flow_definition`、`flow_node`、`flow_skip`、`flow_instance`、`flow_task`、`flow_his_task`、`flow_user` 表，再执行对应的 `20261016_flow` 脚本一次。该脚本只创建 Warm-Flow 1.8.9 的七张引擎表，不新增菜单或业务数据；已有 Warm-Flow 库应单独评估升级，不能直接重跑建表脚本。
14. 流程定义管理接口需要执行对应的 `20261016_flow_definition_permissions` 脚本一次。执行前检查菜单 ID `197~203` 均未占用；脚本先给模板租户添加暂时隐藏的“OA审批 / 流程定义”菜单及五个按钮，并授权默认两种管理员角色。其他租户通过“同步应用菜单”获取，再按需授权角色。流程定义前端上线后再执行 `20261016_flow_definition_menu`，启用模板及已同步菜单，补齐页面组件路径；已经执行 permissions 脚本的库不要重复执行它。

执行第 7 步前，确认数据库中不存在 `upm_third_auth_config` 表、`system/third-auth/index` 组件和 `system:thirdAuth:*` 权限编码。该脚本不是幂等脚本，已经执行成功后不要重复执行。

新数据库只执行 `sql/full/` 中对应数据库的全量脚本，不再执行本次迁移。本增量脚本为每个已有租户创建不可操作的内置应用（编码 `infra`），将已有菜单关联到它；原菜单 ID、`operable`、启停状态、授权关系都保留。新增应用管理菜单及四个按钮默认授予该租户的平台管理员和租户管理员。完整目录及用法见 [SQL 说明](../README.md)。

## 审批中心迁移（20261019）

已完成 Flow 七张表和流程定义菜单迁移的库，执行对应的 `20261019_flow_inbox` 一次。先停写、备份，确认模板租户1的infra应用下恰有一个 `menu_url='flow'` 的OA目录，且尚无 `flow/todo/index`、`flow/done/index`、`flow/started/index` 组件和 `idx_flow_*` 本脚本索引。

脚本使用现有最大菜单ID之后的连续6个ID，新增3个页面和3个查询按钮，并授权默认管理员角色。其他租户通过同步应用菜单获得，普通角色需要按需授权。执行后重新登录刷新菜单。MySQL DDL会隐式提交，部分失败时先核对已生效语句，不要盲目重跑。新库只执行全量脚本。

查询脚本执行后，继续执行对应的 `20261019_flow_task_actions` 一次，增加待办菜单的通过、退回按钮并授权默认管理员角色。确认尚无 `flow:task:pass`、`flow:task:reject` 权限；同样在停写期间从最大ID之后连续分配两个ID。其他租户同步菜单后按需授权。两批脚本均不创建测试流程或业务申请单。

## 核验

### 审批中心旧脚本出现1137错误时

旧版 `20261019_flow_inbox`、`20261019_flow_task_actions` 曾在同一条 `UNION ALL` 语句中重复读取临时表，MySQL会报 `Can't reopen table`。修正版拆成逐条 `INSERT ... SELECT`，保留同一临时表中的起始ID和父子关系；MySQL/PostgreSQL增量与全量脚本均已同步。

如果客户端在第一条菜单INSERT报错后立即停止、没有继续执行，且仍在同一数据库连接：临时表已创建，但报错的整条INSERT没有插入菜单。直接从修正版脚本的第一条 `INSERT INTO upm_permission` 开始执行到末尾，不要重复执行开头的 `CREATE TEMPORARY TABLE`。

如果客户端继续执行了后续语句，或者已断开连接，不要盲目重跑整份脚本。先检查模板租户中三个页面及五个按钮是否存在，并核对脚本末尾五个索引。临时表已消失且本次菜单/按钮均不存在时，可重新执行创建临时表及插入、授权部分；索引只执行尚不存在的语句。存在部分菜单或授权时先核对已生效范围，不要重新计算起始ID或删除既有菜单后重跑。

```sql
SELECT id, parent_id, component, permission_code
FROM upm_permission
WHERE tenant_id = 1
  AND (component IN ('flow/todo/index', 'flow/done/index', 'flow/started/index')
       OR permission_code IN ('flow:task:todo', 'flow:task:done', 'flow:instance:mine',
                              'flow:task:pass', 'flow:task:reject'));

SHOW INDEX FROM flow_instance WHERE Key_name = 'idx_flow_instance_mine';
SHOW INDEX FROM flow_his_task WHERE Key_name IN ('idx_flow_history_approver', 'idx_flow_history_instance');
SHOW INDEX FROM flow_task WHERE Key_name = 'idx_flow_task_instance';
SHOW INDEX FROM flow_user WHERE Key_name = 'idx_flow_user_candidate';
```

### MySQL 5.7 在旧版 ROW_NUMBER 语句处失败时

MySQL 脚本已改为兼容 5.7 的关联计数，不再使用窗口函数。**不要从头重跑**：前面的建表、加列和数据更新可能已经生效。以下续执行说明仅适用于旧脚本在创建 `migration_application_menu` 临时表时失败、尚未成功插入任何应用管理菜单的情况。

停写、备份后，先执行只读检查：

```sql
-- 应为0：前半段已为所有租户创建内置应用。
SELECT COUNT(*) AS missing_builtin_application
FROM upm_tenant t
LEFT JOIN upm_application a ON a.id = t.id AND a.tenant_id = t.id
    AND a.code = 'infra' AND a.deleted = 0
WHERE a.id IS NULL;

-- 应为0：原菜单已关联对应租户的内置应用。
SELECT COUNT(*) AS unmapped_permission
FROM upm_permission WHERE app_id IS NULL OR app_id <> tenant_id;

-- 必须为0才可按下述位置续执行；含已删除记录，避免重复创建。
SELECT COUNT(*) AS existing_application_permission
FROM upm_permission
WHERE component = 'system/application/index'
   OR permission_code LIKE 'system:application:%';

-- 客户端可能在报错后继续执行了结尾的索引语句。
SHOW INDEX FROM upm_permission WHERE Key_name = 'idx_permission_app_device';
```

前三项都为 0 时，在新的数据库连接中，从修订后的 MySQL 脚本 `CREATE TEMPORARY TABLE migration_application_menu AS` 开始，按顺序执行剩余语句。临时表创建、菜单插入、角色授权和临时表删除必须使用同一连接。

如果索引检查已有结果，跳过最后的 `CREATE INDEX idx_permission_app_device ...`；`MODIFY COLUMN app_id BIGINT NOT NULL` 可在字段已为非空时执行。任一计数不为 0，先核对实际执行到哪一步，不要继续插入，也不要删除现有记录后重跑。

### 迁移完成后的检查

Flow 脚本执行后，应存在 `flow_definition`、`flow_node`、`flow_skip`、`flow_instance`、`flow_task`、`flow_his_task`、`flow_user` 七张表；不要因为官方包中存在可选的表单 Mapper，就手工推测并补建 `flow_form`。表结构以 1.8.9 官方 SQL 为准。

```sql
-- 应为0：菜单所属应用与租户必须匹配。
SELECT COUNT(*) FROM upm_permission p
LEFT JOIN upm_application a ON a.id = p.app_id AND a.tenant_id = p.tenant_id
WHERE a.id IS NULL;

-- 每个未删除租户应有一个启用、不可操作的infra应用。
SELECT t.id, a.id, a.operable, a.usable FROM upm_tenant t
LEFT JOIN upm_application a ON a.tenant_id = t.id AND a.code = 'infra' AND a.deleted = 0
WHERE t.deleted = 0;

-- 每个未删除租户应有一个第三方认证菜单及四个按钮。
SELECT t.id, COUNT(p.id) AS permission_count FROM upm_tenant t
LEFT JOIN upm_permission p ON p.tenant_id = t.id
    AND (p.component = 'system/third-auth/index'
         OR p.permission_code LIKE 'system:thirdAuth:%')
    AND p.deleted = 0
WHERE t.deleted = 0
GROUP BY t.id;
```

## 行为及接入边界

- 新建应用只创建空应用，不复制菜单；编码在租户内唯一且不可修改。
- 新建租户复制租户 1 所有未删除应用、设备端、菜单按钮，包括停用记录；重新生成 ID，记录菜单模板来源并重建父子关系。只创建租户管理员，不再为每个租户创建平台管理员。
- 租户 1 的平台管理员可以在租户管理中预览并同步后续应用菜单变化。应用按不可变编码对应，菜单按模板来源对应；租户自建菜单不受影响，租户管理员的平台菜单授权与模板保持一致。
- 菜单新增、编辑必须提交 `appId`；查询的 `appId` 用于管理范围，不能改变接口鉴权应用。
- 角色权限单范围接口增加必填 `appId`；批量接口 `/role/bindPermissions` 接收 `{roleId, scopes: [{roleId, appId, device, permissionIdList}]}`，仅替换明确提交的范围。空列表表示清空该范围；未提交范围保持原样。
- 接口权限以服务端 `upm.application-code` 配置为准，默认 `infra`，设备端来自登录态。应用被停用后，该应用对应服务的受保护请求被拦截。不要让客户端 header 或查询参数设置此配置。
- 权限读取不再复用旧的跨应用权限编码缓存，菜单/按钮变化直接从当前授权查询读取。此轮以正确隔离为先，不额外引入缓存失效体系。
- SSO 使用 Sa-Token 模式三。独立业务服务不共享 UPM Redis，通过签名校验一次性 ticket 后签发自己的业务 token；详细流程与安全边界见 `doc/SSO.md`。
- 第三方认证配置按租户隔离，Client Secret第一阶段明文存储但不会通过查询接口返回；新租户只复制平台占位配置，不复制任何连接凭据且默认停用。详细边界见 `doc/THIRD_AUTH.md`。
- 账号密码登录风控按租户配置，失败次数和临时锁定状态保存在Redis。策略记录不存在时不限制；新租户默认创建“10分钟内失败3次、锁定10分钟”的策略，钉钉和SSO登录不参与计数。
- 具备 `system:user:unlock` 权限的管理员可在用户管理中清除指定用户的临时锁定和失败次数；操作仍受租户隔离和用户数据范围约束。
- 具备 `system:user:batch:add` 权限的管理员可一次新增最多500个用户；未通过账号、关联数据或部门数据权限校验的行不会写入。
- 租户上下文会校验登录用户：普通用户只能访问自身租户，租户 1 的平台管理员可显式选择其他租户。`/user/current` 始终返回用户自己的租户信息。

## 回退

保留部署前备份。新租户/新应用产生数据后，不应直接删除应用列回退；停止写入后恢复配套数据库备份及前后端版本。迁移脚本自身不删除原有业务数据。
