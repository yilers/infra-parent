# infra-parent

```
JDK25
SpringBoot4.0.2

1. 租户
2. 菜单
3. 角色
4. 职位
5. 部门
6. 用户
7. 日志
```

## 1. 本地运行
```
1. 拉取代码执行sql
2. 修改数据库 redis等配置
3. 启动服务后访问 http://localhost:9000/index.html
4. 平台管理员登录：admin@yilers.com / yilers@123
5. 内置 平台管理员:platform@yilers.com 租户管理员:admin@yilers.com
 > 平台管理员可管理租户及菜单按钮等配置
 > 租户管理员管理用户 角色配置等业务型配置
 > 当创建一个新租户时，比如租户code为 baidu.com ，那么会同步创建出这个租户的租户管理员角色 并创建出admin@baidu.com 为租户管理员
 > 目前会同步默认租户的菜单和默认角色的权限关联 同步到新租户，如果后面新加菜单 需要添加在租户同步逻辑 处理新租户看不到新菜单问题
6. 本项目为基础服务，单独部署，与业务服务分离 互不干扰
 > 目前start作为upm的启动器 如果是微服务 那么只需要与业务服务相互调用比如添加feign或者dubbo组件
 > 但是目前我更推荐 模块开发 共同依赖启动。比如现在这个服务jar可以直接引入到新的业务服务 业务服务的启动会把upm功能集成
7. 业务服务可以引用本服务 upm-api upm-core即可使用RBAC全部功能并使用api接口
8. SOA架构 业务创建推荐创建api core模块，引入上述连个模块 start仅作为启动入口
9. 推荐业务数据表创建为 项目_表名，例如 upm_user
10. 等到常用依赖服务支持springboot4后发布中央仓库 暂时本地安装私服

 目前角色数据权限包含看全部 本部门及以下 本部门 自定义部门
 仅看自己目前归于用户下 单独给某个用户 某个接口分配，这里后续支持针对接口 配角色下所有数据权限及仅自己 处理器已经支持
 
 当接口需要权限控制时 特别是需要仅自己配置时 参考下面
 > 默认数据权限通过表中dept_id字段过滤，当多表关联时，tableName为指定主表
 > 默认除过仅自己是通过dept_id 但是当表中命名没有按照这样时 可以指定
 > userField就是指定仅自己时这张业务表创建人的id，默认是 create_id，像下面日志例子中创建人字段是operator 所以指定
```
```java
@InterceptorIgnore(tenantLine = "true", dataPermission = "false")
@DataPermission(tableName = "upm_log", deptField = "dept_id", userField = "operator")
Page<LogInfoResponse> findByPage(@Param("page") Page<?> p,
                                 @Param("request") BasePageRequest<Log> request);
```

![loading](./img/01.png)
![登录页](./img/02.png)
![首页](./img/03.png)
![角色分配菜单权限](./img/04.png)
![用户单独授权数据权限](./img/05.png)
![日志列表](./img/06.png)

```
.
├── bin
│   ├── deploy-central.sh
│   ├── deploy-company.sh
│   └── install.sh
├── common
│   ├── common-auth
│   │   ├── src
│   │   │   └── main
│   │   └── pom.xml
│   ├── common-core
│   │   ├── src
│   │   │   └── main
│   │   └── pom.xml
│   ├── common-redisson
│   │   ├── src
│   │   │   └── main
│   │   └── pom.xml
│   ├── common-web
│   │   ├── src
│   │   │   └── main
│   │   └── pom.xml
│   └── pom.xml
├── sql
│   └── 01.初始化.sql
├── upm
│   ├── upm-api
│   │   ├── src
│   │   │   └── main
│   │   └── pom.xml
│   ├── upm-core
│   │   ├── src
│   │   │   ├── main
│   │   │   └── test
│   │   └── pom.xml
│   ├── upm-start
│   │   ├── src
│   │   │   ├── main
│   │   │   └── test
│   │   └── pom.xml
│   └── pom.xml
├── LICENSE
├── README.md
└── pom.xml

```

```bash
brew install tree

tree -L 4 -I "node_modules|.git|target|.idea|log|img" --dirsfirst

find . -maxdepth 3 \
  -not -path "./.git*" \
  -not -path "./node_modules*" \
  -not -path "./target*" \
  -not -path "./.idea*" \
  | sed 's|[^/]*/|│   |g;s|│   \([^│]\)|├── \1|'
```