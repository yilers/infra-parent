# infra-parent 项目上下文

## 项目概述

infra-parent 是一个基于 Spring Boot 4.0.3 和 JDK 25 的基础服务项目，主要提供完整的 RBAC（基于角色的访问控制）权限管理系统。该项目设计为独立部署的基础服务，可与业务服务分离，也可作为模块集成到业务服务中。

### 核心功能
- 租户管理（多租户支持）
- 菜单管理（支持菜单按钮配置）
- 角色管理
- 职位管理
- 部门管理
- 用户管理
- 日志管理

### 技术栈
- **Java 版本**: JDK 25
- **Spring Boot**: 4.0.5
- **数据库**: MySQL（使用 MyBatis Plus 3.5.16）
- **缓存**: Redis（使用 Redisson 4.1.0）
- **认证授权**: Sa-Token 1.44.0
- **工具库**: Hutool v7.0.0-M4
- **API 文档**: Knife4j 4.5.0
- **动态线程池**: Dynamic-TP 1.2.2
- **任务调度**: PowerJob 5.1.2
- **缓存框架**: JetCache 2.7.8
- **IP 地域查询**: ip2region 3.3.4

## 项目结构

```
infra-parent/
├── common/                    # 公共模块
│   ├── common-auth/          # 认证授权模块（基于 Sa-Token）
│   ├── common-core/          # 核心工具模块（Hutool、Lombok）
│   ├── common-redisson/      # Redisson 配置模块
│   └── common-web/           # Web 相关模块（MyBatis Plus、Knife4j 等）
├── upm/                       # 用户权限管理模块
│   ├── upm-api/              # API 接口定义
│   ├── upm-core/             # 核心业务逻辑实现
│   └── upm-start/            # 启动器模块
├── bin/                       # 部署脚本
│   ├── install.sh           # 本地安装脚本
│   ├── deploy-central.sh    # 部署到中央仓库脚本
│   └── deploy-company.sh    # 部署到公司私服脚本
├── sql/                       # 数据库脚本
│   └── 01.初始化.sql         # 数据库初始化脚本
└── pom.xml                   # 父 POM 文件
```

## 模块依赖关系

```
upm-start
  └── upm-core
        ├── common-auth
        │     └── common-core
        └── upm-api
              └── common-web
                    ├── common-core
                    ├── common-redisson
                    └── (Spring Boot Web 相关依赖)
```

## 构建和运行

### 环境要求
- JDK 25
- Maven 3.x
- MySQL 8.0+
- Redis 6.0+

### 本地运行

1. **初始化数据库**
   ```bash
   # 执行数据库初始化脚本
   mysql -u root -p < sql/01.初始化.sql
   ```

2. **修改配置文件**
   - 编辑 `upm/upm-start/src/main/resources/application-dev.properties`
   - 配置数据库连接、Redis 连接等

3. **编译安装**
   ```bash
   # 方式1：使用提供的脚本（推荐）
   cd bin
   ./install.sh
   # 按提示输入版本号，如 1.0.0

   # 方式2：直接使用 Maven 命令
   mvn clean install -Drevision=1.0.0
   ```

4. **启动服务**
   ```bash
   # 进入启动模块目录
   cd upm/upm-start

   # 使用 Maven 启动（开发环境）
   mvn spring-boot:run -Dspring-boot.run.profiles=dev

   # 或使用 Java 启动打包后的 jar
   java -jar target/upm-start-1.0.0.jar --spring.profiles.active=dev
   ```

5. **访问应用**
   - 前端地址：http://localhost:9000/index.html
   - API 文档：http://localhost:9000/doc.html（Knife4j）
   - 平台管理员账号：`admin@yilers.com` / `yilers@123`
   - 租户管理员账号：`platform@yilers.com`

### 打包部署

```bash
# 打包（跳过测试）
mvn clean package -DskipTests -Drevision=1.0.0

# 部署到本地仓库
mvn clean install -DskipTests -Drevision=1.0.0

# 部署到公司私服
mvn clean deploy -Pcompany -Drevision=1.0.0

# 部署到中央仓库（需要 GPG 签名）
mvn clean deploy -Pcentral -Drevision=1.0.0
```

### 环境配置

项目支持多环境配置，通过 `spring.profiles.active` 切换：

- `dev` - 开发环境
- `test` - 测试环境
- `gray` - 灰度环境

配置文件位置：`upm/upm-start/src/main/resources/application-{profile}.properties`

## 开发约定

### 架构模式
- **SOA 架构**：推荐业务服务创建 `api` 和 `core` 模块
- **模块化开发**：业务服务可直接引入 `upm-api` 和 `upm-core` 模块使用 RBAC 功能
- **启动器分离**：`upm-start` 仅作为启动入口，业务服务可创建自己的启动器

### 数据库规范
- 表命名规范：`项目_表名`，例如 `upm_user`
- 默认数据权限通过 `dept_id` 字段过滤
- 逻辑删除字段：`usable`（1-已删除，0-未删除）
- ID 生成策略：MyBatis Plus 的 `assign_id`（雪花算法）

### 权限控制
- **数据权限类型**：
  - 看全部
  - 本部门及以下
  - 本部门
  - 自定义部门
  - 仅看自己（通过 `@DataPermission` 注解配置）

- **接口权限注解示例**：
  ```java
  @InterceptorIgnore(tenantLine = "true", dataPermission = "false")
  @DataPermission(tableName = "upm_log", deptField = "dept_id", userField = "operator")
  Page<LogInfoResponse> findByPage(@Param("page") Page<?> p,
                                   @Param("request") BasePageRequest<Log> request);
  ```

### 代码规范
- 编译参数：`-parameters`（保留方法参数名）
- 编码：UTF-8
- 时区：Asia/Shanghai
- 日志级别：com.github.yilers=debug，root=info

### 租户管理
- 平台管理员可管理租户及菜单按钮等配置
- 租户管理员管理用户、角色配置等业务型配置
- 创建新租户时（如租户 code 为 `baidu.com`），会自动创建：
  - 租户管理员角色
  - 租户管理员账号（如 `admin@baidu.com`）
  - 同步默认租户的菜单和角色权限

### 数据权限配置
需要在 `application.properties` 中配置需要数据权限的表：
```properties
permission.column.table=upm_log,upm_user
```

## 关键特性

### 1. 多租户支持
- 基于 Sa-Token 的租户隔离
- 租户管理员自动创建
- 菜单和权限自动同步

### 2. 数据权限
- 支持部门级数据权限控制
- 支持用户级数据权限（仅看自己）
- 可自定义业务表的部门字段和用户字段

### 3. 缓存策略
- 使用 JetCache + Redisson 实现二级缓存
- 支持方法缓存注解
- 权限缓存与业务缓存分离

### 4. 监控和文档
- Knife4j API 文档
- Spring Boot Actuator 健康检查
- 动态线程池监控（Dynamic-TP）

## 常用命令

### Maven 构建
```bash
# 完整构建（包含 Source 和 Javadoc）
mvn clean install -Drevision=1.0.0

# 跳过测试构建
mvn clean install -DskipTests -Drevision=1.0.0

# 仅打包
mvn clean package -DskipTests -Drevision=1.0.0
```

### 开发调试
```bash
# 开发环境启动
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 查看依赖树
mvn dependency:tree

# 查看有效 POM
mvn help:effective-pom
```

## 注意事项

1. **版本管理**：使用 `${revision}` 属性统一管理版本号，构建时通过 `-Drevision` 参数指定
2. **测试跳过**：默认配置跳过测试（`<skipTests>true</skipTests>`），如需运行测试需修改配置
3. **JDK 兼容性**：项目使用 JDK 25，确保开发环境 JDK 版本正确
4. **租户同步**：新增菜单后需要在租户同步逻辑中处理，否则新租户看不到新菜单
5. **Spring Boot 升级**：当前使用 Spring Boot 4.0.3（最新版本），等待常用依赖服务支持后再发布到中央仓库

## 许可证

Apache License 2.0

## 开发者

- zhanghui <jwtpermission@gmail.com>
- GitHub: https://github.com/yilers/infra-parent