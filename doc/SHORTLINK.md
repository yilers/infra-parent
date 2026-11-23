# 短链服务与系统调用接入

短链通过 `shortlink-api` 提供本地 Service 契约，`shortlink-core` 提供业务实现、管理接口、系统调用接口和公开跳转。系统调用凭证由 UPM 管理；签名和接口授权属于公共认证能力。单体启动使用 `infra-start`，短链模块不直接依赖 `upm-core`。

## 安装和部署

1. 新库执行 `sql/full/mysql.sql` 或 `sql/full/postgres.sql`；已有库先备份并停写，按照 `sql/migration/README.md` 的短链迁移说明执行对应增量。不要在全量库重复执行增量。
2. 为每个环境单独生成并妥善备份 32 字节随机主密钥，以 Base64 格式通过环境变量 `UPM_API_SECRET_KEY` 配置，配置到运行 UPM 的进程。不要与 SSO 主密钥共用，也不要提交到仓库：

   ```shell
   openssl rand -base64 32
   ```

   所有使用同一凭证库的 UPM 节点必须配置同一个主密钥。丢失主密钥后无法还原旧 Secret，需要重新创建凭证并更新调用方。
   同时单独生成并备份一个 32 字节 Base64 主密钥，配置到短链服务的 `SHORTLINK_MASTER_KEY`（或 `shortlink.crypto.master-key`），用于加密各 namespace 的打散密钥。所有短链节点必须使用相同配置；该密钥丢失后，已有打散路径将无法生成新短码，旧链接跳转不依赖该密钥。
3. 配置数据库和 Redis。签名防重放、限流必须使用所有节点共享的 Redis；应使用不会提前淘汰 nonce 的实例或配置，否则重放防护不成立。短链跳转缓存与认证存储需要分别规划容量和淘汰策略，不能只靠 Redis 逻辑 DB 隔离淘汰策略。
4. 将租户短链域名解析到短链服务，配置 HTTPS 证书和反向代理。代理应只接受可信域名，并正确传递 Host；短链服务使用实际请求的服务器名匹配已登记域名，不接受客户端租户 ID。当前没有自动 DNS 域名所有权验证，管理员登记前须确认域名归属。
5. 模板租户新菜单包括“系统管理/调用凭证”和“短链管理”。其他租户通过现有租户菜单同步功能获取，再由租户管理员按需授权。新租户不会复制其他租户的凭证、Secret、域名和短链数据。

## 数据与链接规则

- 短链配置位于租户下；一个配置可绑定多个 namespace。域名全局唯一，同一域名下路径唯一。不同租户可使用各自域名下的同名路径。域名可以指定对外端口用于本地开发；生产默认留空。
- namespace 的域名、路径、自动/自定义模式及 6/8 位长度创建后不可修改。新建自动路径由数据库分配号段，使用该路径专属随机密钥按 FF1 对 Base62 序号做一一映射，避免连续短码；号段独立提交，回滚或重启可能跳号。短链删除后仍保留短码与请求标识占位；不要删除号段表或回退高水位。
- 现有自动路径保留旧的顺序算法以避免和历史短码碰撞，旧路径及其链接不自动迁移。若需要打散，使用新的路径段创建 namespace；算法版本、长度及路径密钥一经确定不可修改。FF1 密文只是减少顺序枚举，不能代替目标业务鉴权。
- 各 namespace 打散密钥以 AES-256-GCM 加密保存，不通过管理接口返回；不要直接修改、删除或替换数据库中的密文。更换主密钥需要专门的密钥重包流程，当前尚未提供，不能直接替换配置。
- 支持完整 HTTP/HTTPS URL，也支持配置 `{code}` 模板并传业务编码；业务编码会按 UTF-8 URL 编码后替换，最终目标以快照保存。系统不会请求目标地址。
- 单条创建提供配置内唯一的 `requestId`。批量创建提供配置内唯一的 `batchId`，每条可选 `businessId`（同批不可重复），服务端生成单条幂等标识。整批重试必须保持参数和明细顺序不变，参数改变会报错；批次记录不会随短链删除而释放，已有短链删除后重试会报错。新请求默认生成新短链，不按长链接去重。每批最多 1,000 条，整批事务成功或回滚；已分配号段不会回退。
- 过期时间可显式指定、使用配置默认值或设为永久。续期不能缩短有效期，已停用的链接不会因续期自动启用。
- 短链不是访问凭证。敏感页面、优惠券领取等仍由目标系统鉴权。
- 跳转使用 HTTP 302 和 `Cache-Control: no-store`。Redis 按需缓存命中的链接，最长 30 分钟且不超过短链有效期；修改链接前删除缓存并持有分布式写锁直到事务完成。域名、配置、路径状态每次跳转仍查库；Redis 不可用时跳转回源，变更链接失败关闭。当前不缓存不存在的短码，网关应额外限流。

## 六位、八位短码的容量与选择

短码使用大小写敏感的 Base62 字符集 `0-9A-Za-z`，固定长度为6位或8位。域名、namespace路径不计入短码长度。例如 `https://s.example.com/a/Ab12Cd` 的短码为6位，`/a/` 是独立的路径空间。

| 长度 | 理论组合数 | 当前自动模式可分配序号数 | 容量关系 |
| --- | ---: | ---: | --- |
| 6位 | `62^6 = 56,800,235,584`，约568亿 | `56,800,235,583` | 基准 |
| 8位 | `62^8 = 218,340,105,584,896` | `218,340,105,584,895` | 是6位理论容量的3,844倍 |

理论组合包含全零编码对应的序号0；当前 `shortlink_sequence.max_allocated_id` 初始为0，首次从1开始分配，因此自动模式少用一个组合。FF1把序号一一映射为同长度短码，不增加或减少这个容量，也不保证映射后排除字面上的 `000000`。自定义模式不使用自动号段，但仍受长度、字符集和唯一约束限制。

容量按**每个namespace**分别计算，不是全系统共用。`/a/Ab12Cd` 与 `/b/Ab12Cd` 可以同时存在；不同租户使用不同域名时，也可以各有自己的 `/a/Ab12Cd`。同一域名下不能重复登记相同路径，实际访问地址由域名、路径和短码共同定位。

以下按当前自动序号上限、每日持续生成且一年365天估算，不包含跳号：

| 每个namespace每天生成量 | 6位空间约可使用 | 8位空间约可使用 |
| --- | ---: | ---: |
| 10万条 | 1,556年 | 5,981,921年 |
| 100万条 | 156年 | 598,192年 |

每次占用的是新短码，不是访问次数。可按 `剩余序号数 / 每日序号消耗量` 估算剩余天数，再除以365估算使用年限。号段跳号、失败请求等会消耗空间，实际不能直接按短链记录数推算剩余额度。

一般后台分享、营销活动、订单入口等优先选择6位；单个namespace长期承载海量业务、希望为增长预留更大空间时可以选择8位。长度创建后不能修改，用尽前应新建路径（例如 `/b/`）承接新业务，旧链接继续使用原路径，服务不会在空间耗尽时自动循环覆盖旧码。

**码空间足够不代表数据库能存下同等数量的数据，也不代表已达到某个QPS。** 每条链接还包含目标URL、索引和业务标识；应按生成速度、URL长度、保留年限、数据库和缓存容量单独规划。每天100万条约每秒11.6条只是均值，活动峰值可能高得多。批量创建每次最多1,000条，实际耗时取决于事务、索引、加密和网络，应在目标部署环境压测；访问流量另需评估数据库状态查询、缓存命中率和网关容量。

## 会不会重复，以及重启后如何保证唯一

自动模式不是“随机生成后逐个查库碰撞重试”，而是“数据库分配不重叠号段 → 进程内消费序号 → FF1一一映射 → 数据库唯一约束兜底”。

1. 每次号段最多1,000个，由数据库行锁保护 `shortlink_sequence` 高水位，并在独立事务中提交。各节点领取不同区间，领取成功后才开始使用。
2. 单个序号分配由进程内同步保护；相同namespace、算法版本、长度和密钥下，不同序号会得到不同短码。旧顺序路径同样不重复，但短码连续且容易枚举。
3. 每条创建仍需写数据库，但无需为自动短码逐个查询“是否存在”。同一进程内当前号段用完才领取下一段；多节点分别消费自己的号段。
4. `(namespace_id, short_code)` 唯一索引是最终约束。自定义模式由调用方指定短码，应自行避免重用；重复码无法再次插入，不会覆盖旧链接。

正常停机、进程崩溃或业务事务回滚不会把已领取序号重新分配；未消费的号段余量可能丢弃，表现为跳号。短链过期、停用或逻辑删除都不会释放短码空间，避免旧分享地址日后指向另一个业务。

唯一性依赖以下条件：所有生成节点使用同一份权威号段数据，数据库事务与锁正常工作，不手工回退 `shortlink_sequence.max_allocated_id`，不删除或重建已有namespace的号段记录，不修改已有路径的算法、长度和打散密钥。数据库恢复旧备份时必须核对恢复点后的号段及短码记录，不能仅恢复号段表后继续发号；独立数据库副本同时向同一个对外路径发号也不受当前机制保护。

同一个长链接可以有多个短码，这是业务允许的新建行为，不属于碰撞。相同幂等标识及相同参数重试会返回已有结果；使用新的请求标识则可能新建短链，详见前文请求幂等规则。短码打散只是减少顺序枚举，敏感资源仍由目标业务认证和授权。

## 外部系统调用

在“调用凭证”创建凭证，选择接口能力；Secret 仅创建或重置时展示一次。然后在“短链配置”授权此凭证。凭证绑定当前租户，接口不会接受调用方提供的租户 ID。凭证和权限不缓存，停用或取消授权后下一次请求生效。

首版机器接口只接受无查询参数的 `POST application/json`，最大正文 1 MiB：

| 地址 | 权限 code | 用途 |
| --- | --- | --- |
| `/open-api/shortlink/create` | `shortlink:link:create` | 创建单条 |
| `/open-api/shortlink/batch` | `shortlink:link:create` | 批量创建，请求体为批次对象 |
| `/open-api/shortlink/query` | `shortlink:link:query` | 按 `configId`、`requestId` 查询 |
| `/open-api/shortlink/renew` | `shortlink:link:renew` | 续期 |
| `/open-api/shortlink/disable` | `shortlink:link:disable` | 停用 |

请求需要携带 `X-Api-Key`、`X-Api-Timestamp`（13位毫秒时间戳）、`X-Api-Nonce`（16～64位字母、数字、下划线或横线）和 `X-Api-Signature`（64位小写十六进制）。时间允许偏差 5 分钟；nonce 对同一凭证只能使用一次。请求必须走 HTTPS，不要将 Secret 放进浏览器或请求日志。

批量请求示例（同一批使用同一 namespace，明细可覆盖有效期）：

```json
{"batchId":"order-batch-001","namespaceId":"123","permanent":true,"items":[
  {"businessId":"order-1","targetUrl":"https://example.com/order/1"},
  {"businessId":"order-2","targetUrl":"https://example.com/order/2"}
]}
```

每次 HTTP 重试应使用新的签名时间和 nonce，同时保持批次内容和顺序不变。已有数组形式的 HTTP 批量接口已调整为批次对象；本地 `createBatch(List)` 保留兼容。

签名使用 Sa-Token 的参数排序与签名格式，摘要算法为 SHA-256，**不是 HMAC-SHA256**。对实际发送的 UTF-8 正文字节计算 `bodyHash`，对请求 URI 路径（包含应用 context path，不含域名、查询串）计算 `pathHash`，两者都使用小写十六进制 SHA-256。将下面字段按键名字典序拼接，用 `&` 连接，再拼上 `&key=${secret}`，对整个 UTF-8 字符串做 SHA-256：

```text
accessKey=<Key>&bodyHash=<正文摘要>&method=POST&nonce=<随机值>&pathHash=<路径摘要>&timestamp=<毫秒时间>&version=1&key=<Secret>
```

调用方必须对**实际发送的正文原始字节**签名；JSON 空格、字段顺序或编码变化都会导致验签失败。不能把 Secret 放入 URL 或日志。以下 Java 片段展示算法，发送 HTTP 时应使用相同的 `body` 字节：

```java
byte[] body = "{\"namespaceId\":\"123\",\"requestId\":\"order-001\",\"targetUrl\":\"https://example.com\"}"
        .getBytes(StandardCharsets.UTF_8);
String path = "/open-api/shortlink/create";
String timestamp = Long.toString(System.currentTimeMillis());
String nonce = UUID.randomUUID().toString().replace("-", "");
String bodyHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body));
String pathHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
        .digest(path.getBytes(StandardCharsets.UTF_8)));
Map<String, String> fields = new TreeMap<>();
fields.put("accessKey", accessKey);
fields.put("bodyHash", bodyHash);
fields.put("method", "POST");
fields.put("nonce", nonce);
fields.put("pathHash", pathHash);
fields.put("timestamp", timestamp);
fields.put("version", "1");
StringJoiner joiner = new StringJoiner("&");
fields.forEach((key, value) -> joiner.add(key + "=" + value));
String canonical = joiner + "&key=" + secret;
String signature = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
        .digest(canonical.getBytes(StandardCharsets.UTF_8)));
// 发送 body，并设置上述四个 X-Api-* 请求头；不要记录 canonical 或 secret。
```

验签失败、凭证不可用或重放返回 HTTP 401；没有接口权限返回 403；频率超过限制返回 429。业务校验失败使用项目统一响应格式。每个凭证可以设置每分钟上限，创建后可停用。

忘记 Secret 时可在凭证列表重置：Key、权限和短链配置授权不变，重置后旧 Secret 不再用于新的验签请求，须立即更新调用方；重置前已完成验签的并发请求可能继续执行。若需要平滑轮换，仍建议新建凭证、迁移调用方，再停用旧凭证。

## 本地调用与独立部署

本地业务注入 `ShortlinkService` 直接调用，不执行 HTTP 验签；调用前必须由可信登录态或后台任务建立并在结束后清理租户上下文。异步任务不得从外部参数直接信任租户 ID。

当前权限目录来自当前进程的接口注解，UPM 本地实现凭证查询，`infra-start` 提供租户状态适配。未来拆分部署时需提供可信的凭证认证、租户状态及权限目录适配，并做好服务间认证；仅引用 `common-auth` 或注册服务不代表自动完成这些能力。
