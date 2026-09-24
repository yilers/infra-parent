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

执行第 7 步前，确认数据库中不存在 `upm_third_auth_config` 表、`system/third-auth/index` 组件和 `system:thirdAuth:*` 权限编码。该脚本不是幂等脚本，已经执行成功后不要重复执行。

新数据库只执行 `sql/full/` 中对应数据库的全量脚本，不再执行本次迁移。本增量脚本为每个已有租户创建不可操作的内置应用（编码 `infra`），将已有菜单关联到它；原菜单 ID、`operable`、启停状态、授权关系都保留。新增应用管理菜单及四个按钮默认授予该租户的平台管理员和租户管理员。完整目录及用法见 [SQL 说明](../README.md)。

## 核验

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
- 租户上下文会校验登录用户：普通用户只能访问自身租户，租户 1 的平台管理员可显式选择其他租户。`/user/current` 始终返回用户自己的租户信息。

## 回退

保留部署前备份。新租户/新应用产生数据后，不应直接删除应用列回退；停止写入后恢复配套数据库备份及前后端版本。迁移脚本自身不删除原有业务数据。
