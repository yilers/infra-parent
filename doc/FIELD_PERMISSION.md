# 字段权限

字段权限控制响应中的业务字段是否可见，以及管理接口是否允许修改该字段。它与接口功能权限、行数据权限同时生效：先确定能否访问接口、哪些记录属于可操作范围，再控制字段。此次不修改行数据权限的表结构和默认语义。

## 数据模型

| 表 | 作用 | 主要字段 |
| --- | --- | --- |
| `upm_field` | 代码声明的业务字段目录，按租户隔离 | `tenant_id`、`resource_code`、`resource_name`、`field_code`、`field_name`、`mask_type`、`write_supported`、`sort_number` |
| `upm_role_field` | 角色对字段的查看、修改授权 | `tenant_id`、`role_id`、`field_id`、`view_mode`、`editable` |

`upm_field` 沿用基础审计字段、版本号、启停、可操作和逻辑删除字段。`upm_role_field` 是物理删除的关联表，仅保存租户、角色、字段、查看方式和编辑权限，不继承逻辑删除基类，也不单独维护状态或版本；并发保存由角色版本统一控制。目录以 `(tenant_id, resource_code, field_code)` 普通索引加速查询，由业务保证未删除记录不重复；角色授权仍以数据库唯一键约束 `(tenant_id, role_id, field_id)`。资源编码在同一租户内唯一标识业务资源，例如 `system:user`、`oa:leave`；资源内部字段编码唯一，不与应用绑定。同一个资源的列表、详情、编辑可共用相同字段标识。

目录同步使用READ_COMMITTED事务隔离级别，先锁定租户行，再以当前读查询未删除的同名资源字段：存在则更新，不存在则生成新ID。逻辑删除的历史记录不阻挡重新登记，旧字段ID上的授权不会自动继承，普通角色需要重新授权。新租户创建事务持有新租户行锁，并检查模板目录重复，避免复制出重复字段。不要绕过这些入口直接调用持久化Service或手工插入；普通索引不能阻止绕过业务逻辑的重复写入。

目录来源于代码，不是数据库表名、列名，不提供任意新增字段的管理页面。字段编码应稳定，改名称可以重新同步；改编码等同于新字段，需重新授权。取消某项授权（保存为 `HIDDEN`）会物理删除对应关联，无记录即隐藏；再次授权重新插入，不会被历史删除记录阻挡。删除角色时一并清理字段授权关联，范围外、未提交的字段授权保持不变。

### 查看与修改规则

- `HIDDEN`：JSON 对象中省略字段。
- `MASKED`：按声明脱敏，例如 `138****5678`、`a***@example.com`；原对象和缓存保持原值。
- `FULL`：完整显示。
- `editable`：独立控制修改，只有 `FULL` 且代码声明支持编辑时才能开启。

多个启用角色取权限并集，查看级别取最高，编辑权限取有效授权的并集；没有否决策略。未授权或没有字段策略 Provider 的受保护字段默认隐藏，不影响未添加注解的字段。内置 `platformAdmin`、`tenantAdmin` 角色具有本租户已声明字段的完整查看和修改能力，便于首次配置；仍需通过接口功能权限、租户隔离和行数据权限检查，不能借此跨租户访问数据。

拥有 `system:role:field` 只是允许打开配置接口，保存时还会核验角色归属、平台角色限制、当前人字段授权上限和版本号。当前人不可管理的已有授权保持不变，不允许通过伪造字段 ID 或完整请求扩大权限。配置内置管理员角色不能缩小其上述内置字段权限。

## 安装和页面使用

已有库停写并备份后，选择执行一个增量脚本：

- MySQL：[`20261126_field_permission_mysql.sql`](../sql/migration/20261126_field_permission_mysql.sql)
- PostgreSQL：[`20261126_field_permission_postgres.sql`](../sql/migration/20261126_field_permission_postgres.sql)

空库只执行 [`sql/full/`](../sql/full/) 中对应的全量脚本。脚本创建两张表，为各有效租户的 `system:user` 资源登记手机号、邮箱，并增加角色管理的“字段权限”按钮，默认授权内置管理员。旧 `upm_role_column` 表属于未启用的 SQL 列删除方案，本次删除，不自动转换成新的业务字段授权。

迁移使用现有最大 ID 之后的区间，执行期间必须停止写入。可重复执行：已存在的目录、菜单及管理员菜单关联不会重复插入；`CREATE TABLE IF NOT EXISTS` 不会修复人工更改过的表结构。MySQL DDL 隐式提交，部分失败后需核验两表结构、目录、按钮及授权，再继续执行。核验示例：

```sql
SELECT tenant_id, resource_code, field_code FROM upm_field ORDER BY tenant_id, resource_code, field_code;
SELECT tenant_id, permission_code FROM upm_permission WHERE permission_code = 'system:role:field' AND deleted = 0;
```

重启后端、重新登录后，进入角色管理，在普通角色的更多操作中打开“字段权限”，按业务资源分组配置并保存。打开配置页会同步当前租户的代码目录；目录同步只登记字段，不自动给普通角色授权。新增租户复制模板字段目录并重新生成 ID，不复制其他租户的角色字段授权。

初期试点为用户管理的手机号、邮箱。普通角色升级后默认看不到这两个字段，需由管理员明确授权。列表隐藏不可见列；编辑表单显示完整可见的联系方式，只读字段禁止编辑；批量录入禁用没有编辑权限的联系方式。个人中心修改本人资料、登录和 SSO 使用原有业务规则，不因用户管理 VO 的注解受到影响。

## 业务接入

### 1. 在响应 VO 上声明字段

注解放在返回给前端的 VO/Response DTO 字段或 getter 上，不放数据库 Entity。一个实体用于不同业务时，可使用不同响应类型，避免影响登录、本人资料等接口。

```java
@FieldPermission(resource = "system:user", resourceName = "用户资料",
        field = "phone", name = "手机号", mask = FieldMaskType.PHONE, writeSupported = true)
@Override
public String getPhone() {
    return super.getPhone();
}
```

公共类型位于 `common-api` 的 `io.github.yilers.api.field`。`resource` 使用租户内唯一的业务资源编码，`field` 使用资源内唯一的字段编码，最终标识是 `system:user:phone`，与现有功能权限码保持相同命名风格。它不是 `system:user:list` 等具体接口权限，多个接口可以复用；与 SQL 表名、接口路径和应用无关。`NONE` 不提供脱敏选项，`PHONE`、`EMAIL` 提供相应脱敏。

### 2. 注册代码目录

```java
@Bean
public FieldDefinitionProvider businessFieldDefinitions() {
    return () -> FieldDefinition.fromResponse(BusinessResponse.class);
}
```

显式登记响应类，避免扫描整个类路径或把数据库实体暴露成配置。目录只登记到当前可信租户，不依赖是否创建了应用；同步过程使用租户行锁防止并发重复插入。首次接入或增加字段后，由有权限的管理员打开角色字段权限页同步，也可调用 `POST /field/synchronize`。后台直接返回实体、手工拼字符串或使用其他序列化器不会自动受到该注解保护。

### 3. 在保存处校验写入权限

仅序列化响应不能限制修改。业务必须在加载原记录、校验租户和行范围之后、写数据库之前调用：

```java
fieldPermissionService.checkUpdate("system:user:phone", "手机号",
        request.isPhoneProvided(), original.getPhone(), request.getPhone());
```

请求 DTO 要区分字段未传和显式 `null`：通过字段 setter 记录是否提交，标记字段 `@JsonIgnore`，不允许客户端指定。规则如下：

| 提交情形 | 行为 |
| --- | --- |
| 字段未提交 | 不修改，保留数据库原值 |
| 完整可见且允许修改 | 可以更新或显式清空 |
| 完整可见、只读、原值回传 | 允许，不扩大权限 |
| 完整可见、只读、更改原值 | 拒绝 |
| 隐藏或脱敏字段被提交 | 拒绝，包括显式 null、原值或星号 |

前端提交前移除不可编辑字段，后端独立检查。清空字段需注意 MyBatis Plus 默认忽略 null 更新，用户管理已对显式清空采用条件 `set`，仍保留乐观锁。新增、批量新增、其他写入入口也需采用对应检查，不能只保护编辑接口。

隐藏字段也可能通过筛选数量或排序泄漏信息。用户管理已要求手机号、邮箱完整可见后才能用于筛选、排序；其他业务可调用 `checkQuery` 实现相同限制。

## 接口与模块边界

| 接口 | 权限 | 用途 |
| --- | --- | --- |
| `GET /field/current` | 已登录 | 当前人的最终字段策略；未出现的受保护字段默认隐藏 |
| `POST /field/synchronize` | `system:role:field` | 同步当前租户代码目录 |
| `GET /field/role/{roleId}` | `system:role:field` | 读取角色策略及操作者可授权上限 |
| `POST /field/role/save` | `system:role:field` | 按角色版本保存提交的授权项，未提交项不变 |

`common-web` 只依赖公共字段契约，通过 Jackson 3 序列化扩展保护响应，调用时不修改 VO，不缓存脱敏对象。UPM 的 `FieldPermissionHandler` 实现 `FieldPermissionProvider`，按当前可信租户和用户获取策略。单个 HTTP 请求复用一次策略，后续请求重新读取，不增加 Redis 字段授权缓存；授权变更后下一次请求生效。

独立业务模块可依赖 `common-web/common-api` 并提供自己的 `FieldPermissionProvider`，微服务时可在 Provider 中调用远程权限服务，无需让公共模块直接依赖 UPM。业务实体、内部 Service 返回值、数据库查询本身、日志、文件导出不会因为 HTTP 响应注解自动受到保护；这些入口需显式应用权限或使用经过验证的响应序列化流程。

## 验证建议

先给普通角色只授予手机号脱敏、邮箱隐藏，确认用户列表 JSON 不包含邮箱，手机号原值不会出现在 JSON 中。再分别测试完整只读、允许修改、多角色并集、隐藏字段伪造提交、省略字段编辑不覆盖、显式清空、过期版本，以及其他租户的角色 ID/字段 ID。内置管理员仍可正常新增、修改用户和配置普通角色。
