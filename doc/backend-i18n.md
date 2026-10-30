# 后端消息国际化

后端消息按模块维护，公共消息和各业务模块消息分别放在所属模块，避免公共模块依赖业务模块：

- `common/common-web/src/main/resources/i18n/common-messages.properties`：公共中文消息。
- `common/common-web/src/main/resources/i18n/common-messages_en.properties`：公共英文消息。
- `upm/upm-core/src/main/resources/i18n/upm-messages.properties`：UPM 中文消息。
- `upm/upm-core/src/main/resources/i18n/upm-messages_en.properties`：UPM 英文消息。
- `flow/flow-core/src/main/resources/i18n/flow-messages.properties`：工作流中文消息。
- `flow/flow-core/src/main/resources/i18n/flow-messages_en.properties`：工作流英文消息。
- `shortlink/shortlink-core/src/main/resources/i18n/shortlink-messages.properties`：短链中文消息。
- `shortlink/shortlink-core/src/main/resources/i18n/shortlink-messages_en.properties`：短链英文消息。
- `common/common-auth/src/main/resources/i18n/auth-messages.properties` 及 `_en.properties`：系统调用鉴权公开提示。

每个文件使用注释按业务分类，公共文件先放服务通用消息，再放数据操作等分类；其他模块可以按相同方式添加自己的资源文件。中英文文件的键应保持一致。

## 请求与兼容性

客户端通过 `Accept-Language` 指定语言，例如 `zh-CN` 或 `en-US`。不传或没有支持的语言时使用简体中文；消息键缺失时使用消息定义中的默认中文。响应结构和错误码不变。

当前已接入公共保存失败、更新冲突，账号密码登录、登录锁定及用户管理中的部分业务异常，以及租户、部门、角色、菜单、应用、SSO 和第三方认证中的业务异常；对应表单的部分参数校验、用户批量录入的逐行校验结果也已接入。历史的 `new CommonException("中文提示")` 保持原行为，不会自动翻译；依赖库直接返回的异常和其他尚未接入的消息也不会自动翻译。本次不改变未预期异常的处理方式。

工作流已接入请假申请、待办、抄送记录、审批流转、流程定义、动态办理人、节点抄送配置和部分请求参数校验的业务异常。动态规则作为错误消息参数时可翻译；设计器用作类型标识的值、引擎返回的原始错误详情及其他尚未接入的异常不保证翻译。工作流资源和校验消息目录位于 `flow-core`，不依赖 UPM。

短链业务提示在 `shortlink-core` 单独维护。公共层已对 `NotNull`、`NotBlank`、`NotEmpty`、`Size`、`Min`、`Max`、`Positive` 和 `Pattern` 的标准校验模板按请求语言翻译；其他标准模板仍由校验框架处理，不保证中文回退。系统调用签名鉴权提示在 `common-auth` 内维护，不反向依赖 `common-web`。系统调用仍使用原有 HTTP 错误响应和脱敏策略，错误页面是否展示 `sendError` 消息取决于服务端错误页面配置。

## 增加消息

在所属模块的中英文资源文件对应分类下添加相同的键，再在该模块实现 `MessageCode`，指定键、默认中文和资源文件路径。公共消息参考 `CommonMessage`，业务消息参考 `UpmMessage`。抛出异常时使用：

```java
throw new CommonException(UpmMessage.USER_NOT_FOUND);
```

需要自定义响应码或插入参数时，可分别使用 `new CommonException(403, messageCode)` 和 `new CommonException(messageCode, argument)`；消息文件用 `{0}`、`{1}` 表示参数。参数格式遵循 Java `MessageFormat`，英文中需要显示单引号时应按其规则转义。

参数本身也可以传 `MessageCode`，解析时会按同一请求语言翻译；无参数时不执行模板格式化，因此像 `{code}` 这样的业务占位符会原样保留。

解析器使用 Spring 的消息资源机制，但不替换业务应用自身的 `MessageSource`。新增模块的消息由该模块维护资源和消息定义，不需要修改公共模块的资源列表。

## 兼容原有校验文本

已有 DTO 的注解和批量录入结果仍使用原有中文，避免影响非 HTTP 调用。`MessageTextCatalog` 将明确登记的原文映射到消息定义；HTTP 参数校验异常、已登记的 `IllegalArgumentException` 及批量录入接口在返回时翻译，未登记的文本保持原样，异常状态码不变。UPM 的示例见 `UpmTextMessage` 和 `UpmMessageTextCatalog`。

这是对已有文本的兼容机制：修改校验原文时应同步修改消息定义及中英文资源，并检查原文是否与其他目录冲突。新增业务异常仍优先使用 `MessageCode`，不要依赖中文文本匹配。
