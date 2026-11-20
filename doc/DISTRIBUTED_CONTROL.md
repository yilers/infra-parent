# 分布式锁与限流

`common-redisson` 提供 `@DistributedLock` 和 `@DistributedRateLimiter`。宿主应用通过 `@EnableRedisson` 装载切面，并配置 `RedissonClient`。`infra-start` 已启用此能力。

两个注解的 `key` 均使用完整 SpEL 表达式；不存在“包含 `#` 才解析”的特殊规则。表达式来自开发者编写的注解，不应由请求参数或其他不可信输入提供。

## 固定 key 与动态 key

```java
// Java双引号是注解字符串，内部单引号表示SpEL字符串常量。
@DistributedLock(key = "'infra:lock:order:submit'")
public void submit() {
    // 所有调用者共用这把锁。
}

@DistributedLock(key = "'infra:lock:order:' + #orderId")
public void submitOrder(Long orderId) {
    // 不同订单使用不同的锁。
}

@DistributedRateLimiter(
    key = "'infra:rate:report:export'",
    count = 10,
    message = "请求过于频繁，请稍后重试"
)
public void exportReport() {
    // 使用相同key的所有节点共享限流额度。
}
```

锁和限流应使用不同的业务前缀，避免不同Redis对象复用同一个名字。多节点互斥或共享额度要求相关节点连接同一套Redis、同一个逻辑库，并使用相同的key规则。

## 表达式写法

| 写法 | 含义 |
| --- | --- |
| `'infra:lock:order'` | 固定字符串，内部单引号不可省略 |
| `'infra:lock:order:' + #orderId` | 读取方法参数 |
| `'infra:lock:order:' + #request.id` | 读取参数对象属性 |
| `'infra:lock:order:' + #arg0` | 第一个方法参数，不依赖参数名 |
| `#p0`、`#a0` | 与 `#arg0` 相同的位置别名 |
| `#request?.id ?: 'unknown'` | 安全访问和空值回退；业务是否允许回退需自行确定 |
| `T(java.lang.Math).abs(-1)` | 调用静态方法；即使不含 `#` 也会解析 |

实际参数名需要编译时保留 `-parameters`，本项目父POM已配置。无法保留参数名的外部项目可以使用 `#arg0/#p0/#a0`。切面会解析代理对应的具体实现方法，以使用实现方法上的参数名。

两个注解均不提供 `@beanName` 的Bean解析能力，也没有 Spring Cache 专属的 `#root.methodName` 等上下文。不要直接复制其他注解的表达式而假设上下文完全相同。

表达式结果会转换为字符串，结果为 `null`、空字符串、纯空白，或表达式语法及属性访问错误时，直接抛出 `IllegalArgumentException`；不会访问Redis或执行业务，也不会把错误表达式原样作为key。

## 从服务端身份获取租户与用户

对于已登录的UPM接口，可以这样按租户和用户分别限流：

```java
@DistributedRateLimiter(
    key = "'infra:rate:report:export:'"
        + " + T(io.github.yilers.web.context.RequestContextHolder).getTenantId()"
        + " + ':' + T(cn.dev33.satoken.stp.StpUtil).getLoginIdAsLong()",
    count = 2
)
public void exportMyReport() {
    // 每个租户下的每个用户拥有独立额度。
}
```

这里的租户和用户必须已由服务端认证恢复。匿名接口不能调用要求登录的 `getLoginIdAsLong()`；机器调用接口也没有用户登录态，应使用其可信调用凭证上下文。租户为null时，字符串拼接仍可能产生包含 `null` 的非空key，所以业务必须先保证上下文完整。

锁也可以使用同样的 `T(...)` 写法。锁定共享资源时，key一般应包含“租户+资源ID”，不应加入操作者ID，否则两个用户操作同一资源时会拿到不同的锁。

## 锁参数和事务

- `waitTimeout`：获取锁的最长等待时间，默认 `0`，即不等待；超时获取失败时不会执行方法。
- `waitTimeoutUnit`：等待时间单位，默认 `TimeUnit.SECONDS`，可以独立设为 `TimeUnit.MILLISECONDS`。
- `lockTime`：固定持有时间，默认 `-1`，使用Redisson看门狗续期；指定正数后到期会释放，应确保业务不会在锁到期后仍继续修改资源。
- `lockTimeUnit`：固定持有时间单位，默认 `TimeUnit.SECONDS`；自动续期模式不使用此单位。
- 方法返回或抛出异常后，在 `finally` 中释放当前线程持有的锁。

例如等待最多200毫秒，获取成功后固定持有30秒：

```java
@DistributedLock(
    key = "'infra:lock:order:' + #orderId",
    waitTimeout = 200,
    waitTimeoutUnit = TimeUnit.MILLISECONDS,
    lockTime = 30,
    lockTimeUnit = TimeUnit.SECONDS
)
public void processOrder(Long orderId) {
    // 获取不到锁时抛出异常，不进入此方法；不需要等满200毫秒才开始执行。
}
```

`TimeUnit` 使用 `java.util.concurrent.TimeUnit`。两个单位互相独立，不会因为修改等待单位而改变锁的持有时间。等待时间必须非负；新注解和独立单位工具方法的固定持有时间只允许 `-1` 或正数，不接受 `0` 及其他负数。时间在调用Redisson前转换为毫秒，正数转换后不足1毫秒时会拒绝，避免被截断成0。默认省略所有时间字段时，仍是不等待获取锁、自动续期、方法结束后释放。

编程式调用同样支持独立单位：

```java
boolean locked = DistributedLockHelper.tryLock(
    lockKey, 200, TimeUnit.MILLISECONDS, 30, TimeUnit.SECONDS
);
if (locked) {
    try {
        // 执行业务。
    } finally {
        DistributedLockHelper.unlock(lockKey);
    }
}
```

`lockAndRun`、`tryLockAsync` 也提供相同顺序的独立单位重载。原有不含单位的工具方法仍按秒解释，并保留非正持有时间启用看门狗的兼容行为。`DistributedLockChain.tryLockAll` 的原有调用仍按秒解释，本次不改变锁链接口。

注解通过Spring AOP生效，方法应经由Spring代理调用；同一对象内部的自调用通常不触发切面。

锁需要覆盖整个数据库事务提交过程。当前没有指定锁切面和事务切面的先后顺序，因此不要假设同一方法同时添加 `@DistributedLock` 与 `@Transactional` 就能保证提交前不释放锁。可以让外层Bean方法加锁，再调用另一个Bean的事务方法；内层事务提交完成后，外层方法才结束并释放锁。

分布式锁提供互斥，不等同于请求幂等。第一次请求结束后重试，仍可能再次执行；需要结合业务唯一约束、请求标识、状态条件更新等机制防止重复创建或重复流转。

## 限流参数与边界

- `count`：指定周期内允许的请求次数，必填且必须大于0，每次调用消耗一个许可。
- `interval`、`intervalUnit`：周期长度及单位，默认1秒；周期必须大于0，换算后不得小于1毫秒。
- `waitTimeout`、`waitTimeoutUnit`：等待额度的最长时长及单位，默认0秒，立即判断；等待时间不能为负数，正数换算后不得小于1毫秒。正数等待会占用当前调用线程，普通HTTP接口建议使用默认值。
- `message`：额度不足或等待超时后抛出的 `RateLimitException` 提示；超限不执行业务，Redis异常直接传播。

```java
@DistributedRateLimiter(
    key = "'infra:rate:export:' + #userId",
    count = 5,
    interval = 1,
    intervalUnit = TimeUnit.MINUTES,
    waitTimeout = 200,
    waitTimeoutUnit = TimeUnit.MILLISECONDS,
    message = "导出过于频繁，请稍后重试"
)
public void export(Long userId) {
    // 周期与等待时长使用独立单位；省略等待参数则超限立即拒绝。
}
```

保留Redisson `RRateLimiter` 的现有算法和 `RateType.OVERALL` 共享模式，不提供算法切换、预热或每次消耗多个许可的注解配置。周期和等待统一按毫秒精度传递。

编程式调用可以使用 `initRateLimiter(key, count, interval, intervalUnit)` 和 `tryAcquireRateLimiter(key, waitTimeout, waitTimeoutUnit)`。原有 `initRateLimiter(key, count)` 仍按1秒初始化，`tryAcquireRateLimiter(key)` 仍立即判断。

当前使用Redisson的 `trySetRate` 初始化限流器，不覆盖已存在的限流配置。修改注解中的次数后，需要另行安排已有Redis限流配置的更新；不能只靠重启就假定新额度生效。配置异常和超额异常的HTTP响应由宿主应用的异常处理器决定，本模块不保证统一映射为429。

限流控制请求进入频率，不是同时执行数量限制；耗时任务如需限制并发量，应结合信号量或执行线程池。

## 旧写法迁移

分布式锁注解的 `waitTime` 改为 `waitTimeout`，`leaseTime` 改为 `lockTime`，两个时间默认仍是秒。使用方需要修改注解字段名并重新编译；不再保留旧注解字段。新增加的两个单位字段可以省略，也可以分别指定。

限流注解的 `permitsPerSecond` 改为 `count`；未配置周期时仍为1秒。删除 `fallbackToPass`，超限统一拒绝。外部使用方需要同步修改注解并重新编译；曾依赖超限放行的接口需要重新评估调用行为。

旧的分布式锁曾将“不包含 `#` 的字符串”原样作为key。升级后需要检查使用方的锁注解：

```java
// 旧固定key写法：升级后会因表达式不合法而拒绝执行。
@DistributedLock(key = "infra:order:submit")

// 新写法：计算出的实际key仍为infra:order:submit。
@DistributedLock(key = "'infra:order:submit'")
```

旧实现中 `key = "'infra:order:submit'"` 会连单引号一起作为Redis名字；统一解析后单引号不再属于实际key。只有 `T(...)`、不含 `#` 的旧表达式也会从字面量变为真正执行的表达式。实际key可能变化，涉及多节点部署时，应协调使用同一业务锁或限流器的节点一起迁移，避免新旧key并存导致互斥失效。

本仓库当前没有业务方法直接使用这两个注解；依赖 `common-redisson` 的外部业务项目仍需自行核查用法。
