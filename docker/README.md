# Docker 部署

本目录提供 `infra-start` 的容器部署示例，包含 UPM、Flow 和 Shortlink，并使用后端内嵌的前端资源。默认连接已有的 MySQL 或 PostgreSQL 及 Redis；也可以叠加配置，由 Compose 启动 MySQL 和 Redis。示例不会自动执行 SQL 或修改已有数据库。

## 准备

在服务器安装 Docker、Docker Compose 插件和 Git，拉取项目源码，并在仓库根目录执行：

```bash
./docker/deploy.sh init
```

脚本只创建缺失的配置，不覆盖已有 `.env` 和 YAML；会提示选择已有外部依赖、Compose 自建 MySQL/Redis，或现有 Redis 的自定义网络。选择结果保存在本机 `docker/.deployment-mode`，不会提交到 Git。已有部署也可执行 `init`，选择与当前环境一致的方式。

如果要使用已有的 `docker/compose.redis.yaml`（例如将应用加入现有 Redis 的 `infra-shared` 网络），先确认文件和网络已存在，执行 `init` 时选择 3。脚本不会创建 Docker 网络，也不会更改现有 Redis 容器；网络与服务名由管理员确认。后续切换部署方式时，先备份现有配置，在 `docker/.deployment-mode` 中将唯一一行改为 `external`、`dependencies` 或 `redis-network`，并核对相应配置；不要在不同模式间直接切换数据卷。

编辑 `.env`，配置数据库、Redis 和主密钥。`.env` 中的 `DB_URL` 和 `REDIS_HOST` 是**从应用容器内部访问**的地址，不能直接写服务器的 `127.0.0.1`。默认的 `mysql` 和 `redis` 是下文可选容器的服务名，使用已有服务时要改成实际可访问的地址。

`application-prod.yml` 和 `redisson.yml` 通过只读挂载加载，不会打进镜像。可以按需调整连接池、日志、Redisson 单机/主从/集群等配置；修改 Redisson 配置后重启应用。数据库 URL、驱动、账号和密码由 `.env` 提供；其他数据库需要自行扩展配置和驱动。

### 密钥和密码

分别执行三次下面的命令，生成三个不同的值，填入 `.env` 中的 `UPM_SSO_SECRET_KEY`、`UPM_API_SECRET_KEY` 和 `SHORTLINK_MASTER_KEY`：

```bash
openssl rand -base64 32
```

数据库和 Redis 密码应使用独立的强随机值。例如 `openssl rand -hex 24` 可生成适合直接填写在 `.env` 里的随机密码。如果使用可选数据库容器，还要设置独立的 `MYSQL_ROOT_PASSWORD`，并确保 `DB_USERNAME` 不是 `root`。

三个主密钥用于保护不同的业务数据，生产环境必须长期保存并备份，**不要在重启或更新时重新生成**；随意更换可能导致已有密文无法解密。`.env` 是明文文件，应限制服务器文件权限，不要提交 Git、发送给他人或将真实配置复制到镜像；有服务器或 Docker 管理权限的人员仍可能读取容器环境变量。示例中的实际配置文件已被 Git 忽略。

## 方式一：使用已有数据库和 Redis

1. 在 `.env` 中填写容器可访问的 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD` 和 `REDIS_DATABASE`。默认示例分别让 Spring Data Redis 和 Redisson 使用相同地址及数据库；如使用主从或集群，按实际架构分别配置两者，确认 JetCache、Sa-Token 等组件连接符合预期。
2. 新建空数据库时，先阅读 [`sql/README.md`](../sql/README.md)，使用数据库客户端手动导入与数据库类型对应的全量脚本。已有数据库不要执行全量脚本，应按 [`sql/migration/README.md`](../sql/migration/README.md) 检查并执行尚未应用的增量脚本。
3. 在仓库根目录构建并启动：

   ```bash
   ./docker/deploy.sh start
   ```

外部 Redis 如未设置密码，可以将 `.env` 的 `REDIS_PASSWORD` 留空，并将 `config/redisson.yml` 中的 `password` 改为 `null`；生产环境应保证 Redis 仅在可信网络可访问并做好访问控制。

### PostgreSQL 配置

使用 PostgreSQL 时，将 `.env` 中 `DB_DRIVER` 改为 `org.postgresql.Driver`，`DB_URL` 改为数据库提供方的 JDBC 地址，例如 `jdbc:postgresql://数据库主机:5432/数据库名`。云数据库应根据提供方要求配置 TLS 和连接参数，勿在文档或聊天中公开连接密码。新建空库使用 `sql/full/postgres.sql`，不要与 MySQL 脚本混用。

Supabase 等已有数据库可能承载其他应用，应先备份，并确认目标库或 schema、权限和脚本影响范围；不要直接在有现存业务数据的 schema 中执行全量脚本。PostgreSQL 的实际功能兼容性应在目标环境验证，不能仅以驱动加载成功作为验收。

## 方式二：同时启动 MySQL 和 Redis

本方式适合没有现成依赖的环境。MySQL 和 Redis 使用独立的 Docker 数据卷，不对宿主机发布端口。`.env` 中保留 `DB_URL` 的主机名 `mysql`、数据库名 `infra` 以及 `REDIS_HOST=redis`，设置全部密码和主密钥后，先启动依赖：

在仓库根目录运行 `./docker/deploy.sh dependencies`，脚本会启动并显示依赖容器；这不会导入 SQL。

确认 MySQL 健康检查通过后，**仅在首次创建的空数据库中**手动导入全量脚本：

在 `docker` 目录执行：

```bash
docker compose -f compose.yaml -f compose.dependencies.yaml exec -T mysql \
  sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -u root "$MYSQL_DATABASE"' < ../sql/full/mysql.sql
```

再启动应用：

回到仓库根目录，执行 `./docker/deploy.sh start`。

MySQL 容器的建库和账号环境变量仅在**首次初始化空数据卷**时生效。之后修改 `.env` 不会自动修改已有数据库的密码；应先在数据库中完成相应变更，再同步更新配置。已有数据时不要重新导入全量脚本，升级请先备份并按增量脚本操作。

## 查看、更新和停止

以下命令从仓库根目录执行，脚本会读取 `init` 时选择的部署方式：

```bash
./docker/deploy.sh status
./docker/deploy.sh stop
./docker/deploy.sh start
./docker/deploy.sh update
curl -fsS http://127.0.0.1:9000/actuator/health
```

`update` 检查已跟踪文件是否有本地改动，然后只允许快进拉取当前分支的上游；拉取后提示确认数据库准备情况，确认才构建并启动。服务器存在本地改动、无上游或不能快进时会停止，不会强制覆盖。未跟踪文件也可能导致 Git 拒绝拉取，需要自行核查。脚本的 `start` 和 `update` 只更新 `infra`，不会自动启动或重建依赖容器；自建依赖需先执行 `dependencies`。

首次使用脚本管理已有部署时，如果已经有外部 Redis 网络配置，应在 `init` 中选择 3；不要选择自建数据库模式。`stop` 只停止应用，不删除容器或数据卷。需要持续查看日志时，可以按原来的 Compose 文件组合执行 `docker compose ... logs -f infra`。

默认仅监听宿主机 `127.0.0.1:9000`，可通过 `.env` 的 `INFRA_BIND_ADDRESS` 和 `INFRA_PORT` 修改。通常由 Nginx 等反向代理对外提供 HTTPS，并限制 `/actuator/` 的外部访问。SSO、钉钉回调和短链域名需配置为实际对外域名，反向代理应正确转发 Host 和协议等信息。

应用健康检查访问 `/actuator/health`；健康检查失败不会让 Docker 自动重启容器，应结合日志排查数据库、Redis 或应用故障。日志同时输出到容器日志和 `infra_logs` 数据卷；容器日志设置了大小及数量限制，文件日志仍需根据实际情况管理和监控磁盘。

更新前先备份数据库和主密钥、查阅版本说明并按需执行增量 SQL。如果 SQL 要求停写，应先执行 `./docker/deploy.sh stop`，完成备份和迁移后再更新；脚本不自动运行 SQL。不要随意使用带 `-v` 的删除命令，否则可能删除数据库、Redis 和日志数据卷。

构建镜像需要访问基础镜像仓库和 Maven 依赖仓库；部署机器无法联网时，需要预先准备镜像及依赖，或在能联网的环境构建并安全分发镜像。
