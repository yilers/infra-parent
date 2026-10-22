package io.github.yilers.upm.thirdauth;

import io.github.yilers.web.exception.CommonException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 第三方认证平台适配器注册表。
 */
@Component
public class ThirdAuthProviderRegistry {

    private final Map<String, ThirdAuthProvider> providerMap;

    public ThirdAuthProviderRegistry(List<ThirdAuthProvider> providers) {
        providerMap = providers.stream().collect(Collectors.toUnmodifiableMap(
                ThirdAuthProvider::platform, Function.identity()));
    }

    public ThirdAuthProvider get(String platform) {
        ThirdAuthProvider provider = providerMap.get(platform);
        if (provider == null) {
            throw new CommonException("暂不支持该第三方认证平台");
        }
        return provider;
    }

    /**
     * 判断当前服务是否已实现指定平台的认证流程。
     *
     * @param platform 平台编码
     * @return 已注册对应认证实现时返回 true
     */
    public boolean supports(String platform) {
        return providerMap.containsKey(platform);
    }
}
