package io.github.yilers.web.i18n;

import org.springframework.stereotype.Component;

import java.util.Map;

/** 公共请求对象的校验提示。 */
@Component
public class CommonMessageTextCatalog implements MessageTextCatalog {

    @Override
    public Map<String, MessageCode> messages() {
        return Map.of(CommonMessage.ID_REQUIRED.getDefaultMessage(), CommonMessage.ID_REQUIRED);
    }
}
