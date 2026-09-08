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
    └── 20260907_application.postgres.sql
```

## 新数据库：执行全量脚本

先创建一个空数据库，将下列命令中的 `infra` 替换为实际数据库名。选择对应数据库的一个脚本，执行一次即可，包含原有基础数据、内置应用及应用管理菜单和按钮，不需要再执行本次增量迁移。

```bash
mysql -u root -p infra < sql/full/mysql.sql
psql -U postgres -d infra -v ON_ERROR_STOP=1 -f sql/full/postgres.sql
```

全量脚本不删除现有表，也不是幂等脚本。不要在已有数据库中执行，避免中途失败或混入初始数据。

## 已有数据库：执行增量脚本

停写、备份，先阅读 [增量迁移说明](migration/README.md)，确认当前版本和前置条件后，选择数据库对应的增量脚本。已执行成功的增量不要重复执行；未来多个版本按时间顺序执行尚未应用的脚本。

```bash
mysql -u root -p infra < sql/migration/20260907_application.mysql.sql
psql -U postgres -d infra -v ON_ERROR_STOP=1 -f sql/migration/20260907_application.postgres.sql
```

如果旧版脚本在 MySQL 5.7 的 `ROW_NUMBER()` 处失败，不能直接重跑整份增量，更不能改用全量脚本；按 [部分失败续执行说明](migration/README.md#mysql-57-在旧版-row_number-语句处失败时)检查已执行的状态后续执行。

## 维护约定

数据库结构或初始数据变化时，同时更新 `full/` 的最新快照并新增相应版本的 `migration/` 脚本。全量脚本应独立可执行，不引用其他脚本，也不要求再叠加历史增量。脚本均由人工执行，不会随服务启动自动运行。
