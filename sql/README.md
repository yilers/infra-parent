# 数据库脚本

全量与增量按数据库当前状态二选一，不要串行执行同一版本的两套脚本。

```text
sql/
├── full/                         # 空数据库：完整建表及初始数据
│   ├── mysql.sql                 # MySQL 5.7 / 8.x
│   └── postgres.sql              # PostgreSQL
└── migration/                    # 已有数据库：按版本增量升级
    ├── README.md                 # 前置检查、部分失败续执行和核验说明
    ├── 20260907_application.mysql.sql
    ├── 20260907_application.postgres.sql
    ├── 20260918_sso.mysql.sql
    ├── 20260918_sso.postgres.sql
    ├── 20260923_third_auth.mysql.sql
    ├── 20260923_third_auth.postgres.sql
    ├── 20260924_third_auth_binding.mysql.sql
    ├── 20260924_third_auth_binding.postgres.sql
    ├── 20260925_tenant_permission_sync.mysql.sql
    ├── 20260925_tenant_permission_sync.postgres.sql
    ├── 20260929_tenant_login_policy.mysql.sql
    ├── 20260929_tenant_login_policy.postgres.sql
    ├── 20260929_user_login_unlock.mysql.sql
    ├── 20260929_user_login_unlock.postgres.sql
    ├── 20260930_user_batch_add.mysql.sql
    ├── 20260930_user_batch_add.postgres.sql
    ├── 20261014_dept_leader.mysql.sql
    ├── 20261014_dept_leader.postgres.sql
    ├── 20261016_flow.mysql.sql
    ├── 20261016_flow.postgres.sql
    ├── 20261016_flow_definition_permissions.mysql.sql
    ├── 20261016_flow_definition_permissions.postgres.sql
    ├── 20261016_flow_definition_menu.mysql.sql
    └── 20261016_flow_definition_menu.postgres.sql
```

审批中心新增 `migration/20261019_flow_inbox.mysql.sql` 和 `migration/20261019_flow_inbox.postgres.sql`，用于已有库的个人审批中心菜单、查询按钮和索引迁移；随后执行对应的 `20261019_flow_task_actions` 添加通过、退回按钮。执行前置要求见增量目录说明。

## 新数据库：执行全量脚本

请假申请另增 `migration/20261019_flow_leave.mysql.sql`、`migration/20261019_flow_leave.postgres.sql`。已有库在完成审批中心迁移后再执行；新库全量已包含业务表、菜单及按钮，不额外执行增量。

节点自动抄送新增 `migration/20261020_flow_cc.mysql.sql`、`migration/20261020_flow_cc.postgres.sql`。已有库在请假迁移后执行一次，创建 `flow_cc` 接收快照表和“抄送我的”菜单及查询按钮；新库全量已包含，不额外执行增量。

申请人拿回与撤销新增 `migration/20261021_flow_instance_actions.mysql.sql`、`migration/20261021_flow_instance_actions.postgres.sql`。已有库在审批中心及请假迁移后执行一次，新增“拿回”和“撤销”按钮；新库全量已包含，不额外执行增量。

待办转办与委派新增 `migration/20261022_flow_task_transfer.mysql.sql`、`migration/20261022_flow_task_transfer.postgres.sql`。已有库在 `20261019_flow_task_actions` 后执行一次，新增“转办”和“委派”按钮；新库全量已包含，不额外执行增量。其他租户同步应用菜单后按需授权。

先创建一个空数据库，将下列命令中的 `infra` 替换为实际数据库名。选择对应数据库的一个脚本，执行一次即可，包含原有基础数据、内置应用、应用管理和第三方认证管理菜单及按钮，不需要再执行本次增量迁移。

```bash
mysql -u root -p infra < sql/full/mysql.sql
psql -U postgres -d infra -v ON_ERROR_STOP=1 -f sql/full/postgres.sql
```

全量脚本不删除现有表，也不是幂等脚本。不要在已有数据库中执行，避免中途失败或混入初始数据。

## 已有数据库：执行增量脚本

停写、备份，先阅读 [增量迁移说明](migration/README.md)，确认当前版本和前置条件后，选择数据库对应的增量脚本。已执行成功的增量不要重复执行；未来多个版本按时间顺序执行尚未应用的脚本。

```bash
mysql -u root -p infra < sql/migration/20260907_application.mysql.sql
mysql -u root -p infra < sql/migration/20260918_sso.mysql.sql
mysql -u root -p infra < sql/migration/20260923_third_auth.mysql.sql
mysql -u root -p infra < sql/migration/20260924_third_auth_binding.mysql.sql
mysql -u root -p infra < sql/migration/20260925_tenant_permission_sync.mysql.sql
mysql -u root -p infra < sql/migration/20260929_tenant_login_policy.mysql.sql
mysql -u root -p infra < sql/migration/20260929_user_login_unlock.mysql.sql
mysql -u root -p infra < sql/migration/20260930_user_batch_add.mysql.sql
mysql -u root -p infra < sql/migration/20261014_dept_leader.mysql.sql
mysql -u root -p infra < sql/migration/20261016_flow.mysql.sql
mysql -u root -p infra < sql/migration/20261016_flow_definition_permissions.mysql.sql
mysql -u root -p infra < sql/migration/20261016_flow_definition_menu.mysql.sql
psql -U postgres -d infra -v ON_ERROR_STOP=1 -f sql/migration/20260907_application.postgres.sql
psql -U postgres -d infra -v ON_ERROR_STOP=1 -f sql/migration/20260918_sso.postgres.sql
psql -U postgres -d infra -v ON_ERROR_STOP=1 -f sql/migration/20260923_third_auth.postgres.sql
psql -U postgres -d infra -v ON_ERROR_STOP=1 -f sql/migration/20260924_third_auth_binding.postgres.sql
psql -U postgres -d infra -v ON_ERROR_STOP=1 -f sql/migration/20260925_tenant_permission_sync.postgres.sql
psql -U postgres -d infra -v ON_ERROR_STOP=1 -f sql/migration/20260929_tenant_login_policy.postgres.sql
psql -U postgres -d infra -v ON_ERROR_STOP=1 -f sql/migration/20260929_user_login_unlock.postgres.sql
psql -U postgres -d infra -v ON_ERROR_STOP=1 -f sql/migration/20260930_user_batch_add.postgres.sql
psql -U postgres -d infra -v ON_ERROR_STOP=1 -f sql/migration/20261014_dept_leader.postgres.sql
psql -U postgres -d infra -v ON_ERROR_STOP=1 -f sql/migration/20261016_flow.postgres.sql
psql -U postgres -d infra -v ON_ERROR_STOP=1 -f sql/migration/20261016_flow_definition_permissions.postgres.sql
psql -U postgres -d infra -v ON_ERROR_STOP=1 -f sql/migration/20261016_flow_definition_menu.postgres.sql
```

如果旧版脚本在 MySQL 5.7 的 `ROW_NUMBER()` 处失败，不能直接重跑整份增量，更不能改用全量脚本；按 [部分失败续执行说明](migration/README.md#mysql-57-在旧版-row_number-语句处失败时)检查已执行的状态后续执行。

## 维护约定

数据库结构或初始数据变化时，同时更新 `full/` 的最新快照并新增相应版本的 `migration/` 脚本。全量脚本应独立可执行，不引用其他脚本，也不要求再叠加历史增量。脚本均由人工执行，不会随服务启动自动运行。
