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
5. 内置 平台管理员 租户管理员 部门管理员
6. 本项目为基础服务，单独部署，与业务服务分离 互不干扰
7. 业务服务可以引用本服务 upm-api upm-core即可使用RBAC全部功能并使用api接口
8. SOA架构 业务创建推荐创建api core模块，引入上述连个模块 start仅作为启动入口
9. 推荐业务数据表创建为 项目_表明，例如 upm_user
10. 等到常用依赖服务支持springboot4后发布中央仓库 暂时本地安装私服
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
│   ├── install.sh
│   ├── deploy.sh
├── pom.xml
├── README.md
├── upm
│   ├── upm-core
│   │   ├── target
│   │   ├── pom.xml
│   │   ├── src
│   ├── upm-api
│   │   ├── target
│   │   ├── pom.xml
│   │   ├── src
│   ├── pom.xml
│   ├── upm-start
│   │   ├── target
│   │   ├── pom.xml
│   │   ├── src
├── common
│   ├── common-web
│   │   ├── target
│   │   ├── pom.xml
│   │   ├── src
│   ├── pom.xml
│   ├── common-auth
│   │   ├── target
│   │   ├── pom.xml
│   │   ├── src
│   ├── common-core
│   │   ├── target
│   │   ├── pom.xml
│   │   ├── src
│   ├── common-redisson
│   │   ├── target
│   │   ├── pom.xml
│   │   ├── src
├── sql

```

```bash
brew install tree

tree -L 3 -I "node_modules|.git|target|.idea" --dirsfirst

find . -maxdepth 3 \
  -not -path "./.git*" \
  -not -path "./node_modules*" \
  -not -path "./target*" \
  -not -path "./.idea*" \
  | sed 's|[^/]*/|│   |g;s|│   \([^│]\)|├── \1|'
```