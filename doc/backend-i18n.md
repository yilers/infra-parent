# 后端消息国际化

后端消息按模块维护，公共消息和各业务模块消息分别放在所属模块，避免公共模块依赖业务模块：

- `common/common-web/src/main/resources/i18n/common-messages.properties`：公共中文消息。
- `common/common-web/src/main/resources/i18n/common-messages_en.properties`：公共英文消息。
- `upm/upm-core/src/main/resources/i18n/upm-messages.properties`：UPM 中文消息。
- `upm/upm-core/src/main/resources/i18n/upm-messages_en.properties`：UPM 英文消息。

每个文件使用注释按业务分类，公共文件先放服务通用消息，再放数据操作等分类；其他模块可以按相同方式添加自己的资源文件。中英文文件的键应保持一致。

## 请求与兼容性

客户端通过 `Accept-Language` 指定语言，例如 `zh-CN` 或 `en-US`。不传或没有支持的语言时使用简体中文；消息键缺失时使用消息定义中的默认中文。响应结构和错误码不变。

当前已接入公共保存失败、更新冲突，以及账号密码登录、登录锁定和用户管理中的部分业务异常。历史的 `new CommonException("中文提示")` 保持原行为，不会自动翻译；批量录入的逐行校验结果、参数校验、第三方异常和其他尚未接入的消息也不会自动翻译。本次不改变未预期异常的处理方式。

## 增加消息

在所属模块的中英文资源文件对应分类下添加相同的键，再在该模块实现 `MessageCode`，指定键、默认中文和资源文件路径。公共消息参考 `CommonMessage`，业务消息参考 `UpmMessage`。抛出异常时使用：

```java
throw new CommonException(UpmMessage.USER_NOT_FOUND);
```

需要自定义响应码或插入参数时，可分别使用 `new CommonException(403, messageCode)` 和 `new CommonException(messageCode, argument)`；消息文件用 `{0}`、`{1}` 表示参数。参数格式遵循 Java `MessageFormat`，英文中需要显示单引号时应按其规则转义。

解析器使用 Spring 的消息资源机制，但不替换业务应用自身的 `MessageSource`。新增模块的消息由该模块维护资源和消息定义，不需要修改公共模块的资源列表。
