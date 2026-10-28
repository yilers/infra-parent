package io.github.yilers.shortlink.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.yilers.auth.api.ApiCallerContext;
import io.github.yilers.auth.api.ApiSignProtocol;
import io.github.yilers.shortlink.entity.*;
import io.github.yilers.shortlink.mapper.*;
import io.github.yilers.shortlink.request.*;
import io.github.yilers.shortlink.response.ShortlinkResponse;
import io.github.yilers.shortlink.support.ShortlinkCodeGenerator;
import io.github.yilers.shortlink.support.ShortlinkCache;
import io.github.yilers.shortlink.support.ShortlinkRules;
import io.github.yilers.web.context.RequestContextHolder;
import io.github.yilers.web.exception.CommonException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

import static io.github.yilers.shortlink.support.ShortlinkRules.*;

/** 本地调用和HTTP调用共享的短链业务；所有入口都必须具有可信租户上下文。 */
@Service
@RequiredArgsConstructor
public class ShortlinkServiceImpl implements ShortlinkService {
    private final ShortlinkConfigMapper configs;
    private final ShortlinkNamespaceMapper namespaces;
    private final ShortlinkDomainMapper domains;
    private final ShortlinkLinkMapper links;
    private final ShortlinkBatchMapper batches;
    private final ShortlinkConfigGrantMapper grants;
    private final ShortlinkCodeGenerator generator;
    private final ShortlinkCache cache;
    private final Validator validator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShortlinkResponse create(ShortlinkCreateRequest request) {
        return createItem(request, null, null);
    }

    private ShortlinkResponse createItem(ShortlinkCreateRequest request, String batchId, String businessId) {
        validate(request);
        ShortlinkNamespace namespace = namespace(request.getNamespaceId());
        // 按配置串行化创建，保证并发幂等；不在事务已报唯一约束冲突后继续执行SQL。
        configs.lockConfiguration(namespace.getConfigId(), tenant());
        ShortlinkConfig config = config(namespace.getConfigId());
        String hash = requestHash(request);
        ShortlinkLink previous = links.selectOne(new LambdaQueryWrapper<ShortlinkLink>()
                .eq(ShortlinkLink::getConfigId, config.getId()).eq(ShortlinkLink::getRequestId, request.getRequestId()));
        if (previous != null) {
            if (!hash.equals(previous.getRequestHash()) || !Objects.equals(batchId, previous.getBatchId())
                    || !Objects.equals(businessId, previous.getBusinessId())) throw new CommonException("requestId已使用且请求参数不同");
            return response(previous);
        }
        // 已删除记录仍占用幂等标识，禁止重新创建不同对象。
        if (links.requestExists(config.getId(), tenant(), request.getRequestId()) > 0) {
            throw new CommonException("requestId对应的短链已删除，请使用新的requestId");
        }
        String target = target(config, request);
        LocalDateTime expireTime = expiration(config, request);
        String code;
        if (AUTO.equals(namespace.getGenerateType())) {
            if (request.getShortCode() != null) throw new CommonException("自动模式不能指定短码");
            code = generator.next(namespace);
        } else {
            code = request.getShortCode();
            if (code == null || code.length() != namespace.getCodeLength()) throw new CommonException("自定义短码长度不正确");
        }
        // 自动模式的号段和固定映射保证不重复，数据库唯一约束作为兜底；自定义码需提示已占用。
        if (CUSTOM.equals(namespace.getGenerateType()) && links.codeExists(namespace.getId(), tenant(), code) > 0) {
            throw new CommonException("短码已使用，不能重复分配");
        }
        ShortlinkLink value = new ShortlinkLink();
        value.setConfigId(config.getId());
        value.setNamespaceId(namespace.getId());
        value.setShortCode(code);
        value.setTargetUrl(target);
        value.setBusinessCode(request.getBusinessCode());
        value.setBatchId(batchId);
        value.setBusinessId(businessId);
        value.setRequestId(request.getRequestId());
        value.setRequestHash(hash);
        value.setExpireTime(expireTime);
        value.setVersion(INITIAL_VERSION);
        value.setUsable(ENABLED);
        value.setOperable(ENABLED);
        value.setDeleted(DISABLED);
        ApiCallerContext.Caller caller = ApiCallerContext.current();
        if (caller != null) value.setCredentialId(caller.credentialId());
        links.insert(value);
        return response(value);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ShortlinkResponse> createBatch(List<ShortlinkCreateRequest> requests) {
        tenant();
        if (requests == null || requests.isEmpty() || requests.size() > BATCH_LIMIT) {
            throw new CommonException("每批需包含1到1000条短链");
        }
        requests.forEach(this::validate);
        // 多配置批量请求按ID顺序预锁，避免两个批次相反顺序持锁造成死锁。
        TreeSet<Long> configIds = new TreeSet<>();
        requests.forEach(request -> configIds.add(namespace(request.getNamespaceId()).getConfigId()));
        configIds.forEach(id -> configs.lockConfiguration(id, tenant()));
        List<ShortlinkResponse> result = new ArrayList<>(requests.size());
        for (ShortlinkCreateRequest request : requests) result.add(create(request));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ShortlinkResponse> createBatch(ShortlinkBatchRequest request) {
        tenant();
        validate(request);
        if (request.isPermanent() && request.getExpireTime() != null) throw new CommonException("永久有效不能同时指定到期时间");
        ShortlinkNamespace namespace = namespace(request.getNamespaceId());
        configs.lockConfiguration(namespace.getConfigId(), tenant());
        config(namespace.getConfigId()); // 校验调用凭证授权，不能只依赖租户过滤。

        Set<String> businessIds = new HashSet<>();
        List<ShortlinkCreateRequest> items = new ArrayList<>();
        StringBuilder fingerprint = new StringBuilder();
        appendFingerprint(fingerprint, request.getNamespaceId(), request.getExpireTime(), request.isPermanent());
        for (int i = 0; i < request.getItems().size(); i++) {
            ShortlinkBatchRequest.Item item = request.getItems().get(i);
            String businessId = item.getBusinessId();
            if (businessId != null && (businessId.isBlank() || !businessIds.add(businessId))) {
                throw new CommonException("批次内业务标识不能为空或重复");
            }
            appendFingerprint(fingerprint, businessId, item.getTargetUrl(), item.getBusinessCode(),
                    item.getShortCode(), item.getExpireTime(), item.getPermanent());
            ShortlinkCreateRequest single = new ShortlinkCreateRequest();
            single.setNamespaceId(request.getNamespaceId());
            // 固定长度摘要避免外部批次号过长，并为同一批次中的每条记录生成稳定标识。
            single.setRequestId("batch:" + ApiSignProtocol.sha256(request.getBatchId()) + ":" + i);
            single.setTargetUrl(item.getTargetUrl());
            single.setBusinessCode(item.getBusinessCode());
            single.setShortCode(item.getShortCode());
            boolean permanent = item.getPermanent() != null ? item.getPermanent()
                    : item.getExpireTime() != null ? false : request.isPermanent();
            single.setPermanent(permanent);
            single.setExpireTime(permanent ? item.getExpireTime() :
                    item.getExpireTime() != null ? item.getExpireTime() : request.getExpireTime());
            validate(single);
            items.add(single);
        }
        String hash = ApiSignProtocol.sha256(fingerprint.toString());
        ShortlinkBatch batch = batches.selectOne(new LambdaQueryWrapper<ShortlinkBatch>()
                .eq(ShortlinkBatch::getConfigId, namespace.getConfigId()).eq(ShortlinkBatch::getBatchId, request.getBatchId()));
        if (batch == null) {
            batch = new ShortlinkBatch();
            batch.setConfigId(namespace.getConfigId());
            batch.setBatchId(request.getBatchId());
            batch.setRequestHash(hash);
            batch.setItemCount(items.size());
            batch.setVersion(INITIAL_VERSION);
            batch.setUsable(ENABLED);
            batch.setOperable(ENABLED);
            batch.setDeleted(DISABLED);
            batches.insert(batch);
        } else if (!hash.equals(batch.getRequestHash())) {
            throw new CommonException("批次标识已使用且请求内容不同");
        }
        List<ShortlinkResponse> result = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            result.add(createItem(items.get(i), request.getBatchId(), request.getItems().get(i).getBusinessId()));
        }
        return result;
    }

    private static void appendFingerprint(StringBuilder result, Object... values) {
        for (Object value : values) {
            String text = value == null ? null : value.toString();
            result.append(text == null ? -1 : text.length()).append(':');
            if (text != null) result.append(text);
        }
    }

    @Override
    public ShortlinkResponse find(Long configId, String requestId) {
        config(configId);
        ShortlinkLink link = links.selectOne(new LambdaQueryWrapper<ShortlinkLink>()
                .eq(ShortlinkLink::getConfigId, configId).eq(ShortlinkLink::getRequestId, requestId));
        if (link == null) throw new CommonException("短链不存在");
        return response(link);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void renew(ShortlinkRenewRequest request) {
        validate(request);
        ShortlinkLink value = link(request.getId());
        LocalDateTime expiration = request.getExpireTime();
        if (request.isPermanent() == (expiration != null)) throw new CommonException("请选择新的到期时间或永久有效");
        if (!request.isPermanent() && (value.getExpireTime() == null || !expiration.isAfter(LocalDateTime.now())
                || expiration.isBefore(value.getExpireTime()))) throw new CommonException("续期不能缩短原有效期");
        cache.invalidateBeforeUpdate(value);
        if (links.renew(value.getId(), tenant(), request.getVersion(), expiration) != 1) {
            throw new CommonException("数据已变化，请刷新后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disable(Long id, Integer version) {
        changeUsable(id, version, DISABLED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enable(Long id, Integer version) {
        changeUsable(id, version, ENABLED);
    }

    private void changeUsable(Long id, Integer version, int usable) {
        if (version == null) throw new CommonException("版本号不能为空");
        ShortlinkLink value = link(id);
        cache.invalidateBeforeUpdate(value);
        value.setVersion(version);
        value.setUsable(usable);
        if (links.updateById(value) != 1) throw new CommonException("数据已变化，请刷新后重试");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Integer version) {
        if (version == null) throw new CommonException("版本号不能为空");
        ShortlinkLink value = link(id);
        cache.invalidateBeforeUpdate(value);
        if (links.markDeleted(id, tenant(), version) != 1) throw new CommonException("数据已变化，请刷新后重试");
    }

    public ShortlinkConfig config(Long id) {
        tenant();
        ShortlinkConfig value = configs.selectById(id);
        if (value == null || !Integer.valueOf(ENABLED).equals(value.getUsable())) throw new CommonException("短链配置不存在或已停用");
        ApiCallerContext.Caller caller = ApiCallerContext.current();
        if (caller != null && grants.selectCount(new LambdaQueryWrapper<ShortlinkConfigGrant>()
                .eq(ShortlinkConfigGrant::getConfigId, id)
                .eq(ShortlinkConfigGrant::getCredentialId, caller.credentialId())
                .eq(ShortlinkConfigGrant::getUsable, ENABLED)) == 0) {
            throw new CommonException("调用凭证未获得该短链配置的授权");
        }
        return value;
    }

    private ShortlinkLink link(Long id) {
        tenant();
        ShortlinkLink value = links.selectById(id);
        if (value == null) throw new CommonException("短链不存在");
        config(value.getConfigId());
        return value;
    }

    private ShortlinkNamespace namespace(Long id) {
        tenant();
        ShortlinkNamespace value = namespaces.selectById(id);
        if (value == null || !Integer.valueOf(ENABLED).equals(value.getUsable())) throw new CommonException("namespace不存在或已停用");
        ShortlinkDomain domain = domains.selectById(value.getDomainId());
        if (domain == null || !Integer.valueOf(ENABLED).equals(domain.getUsable())) throw new CommonException("短链域名不存在或已停用");
        return value;
    }

    private ShortlinkResponse response(ShortlinkLink link) {
        ShortlinkNamespace namespace = namespaces.selectById(link.getNamespaceId());
        if (namespace == null) throw new CommonException("短链路径不存在");
        ShortlinkDomain domain = domains.selectById(namespace.getDomainId());
        if (domain == null) throw new CommonException("短链域名不存在");
        return new ShortlinkResponse(link.getId(), link.getRequestId(), link.getBatchId(), link.getBusinessId(), namespace.getPathCode(), link.getShortCode(),
                domain.getScheme() + "://" + domain.getHost()
                        + (domain.getPublicPort() == null ? "" : ":" + domain.getPublicPort())
                        + "/" + namespace.getPathCode() + "/" + link.getShortCode(),
                link.getExpireTime(), link.getUsable(), link.getVersion());
    }

    private String target(ShortlinkConfig config, ShortlinkCreateRequest request) {
        if (URL.equals(config.getTargetType())) {
            if (request.getBusinessCode() != null) throw new CommonException("URL模式不接受业务编码");
            return validateUrl(request.getTargetUrl());
        }
        if (request.getTargetUrl() != null || request.getBusinessCode() == null || request.getBusinessCode().isBlank()) {
            throw new CommonException("模板模式只接受非空业务编码");
        }
        String code = URLEncoder.encode(request.getBusinessCode(), StandardCharsets.UTF_8).replace("+", "%20");
        return validateUrl(config.getUrlTemplate().replace("{code}", code));
    }

    private LocalDateTime expiration(ShortlinkConfig config, ShortlinkCreateRequest request) {
        if (request.isPermanent() && request.getExpireTime() != null) throw new CommonException("永久有效不能同时指定到期时间");
        if (request.isPermanent()) return null;
        LocalDateTime expiration = request.getExpireTime();
        if (expiration == null && config.getDefaultValidSeconds() != null) {
            expiration = LocalDateTime.now().plusSeconds(config.getDefaultValidSeconds());
        }
        if (expiration != null && !expiration.isAfter(LocalDateTime.now())) throw new CommonException("到期时间必须晚于当前时间");
        return expiration;
    }

    private String requestHash(ShortlinkCreateRequest request) {
        StringBuilder value = new StringBuilder();
        for (Object part : Arrays.asList(request.getNamespaceId(), request.getTargetUrl(), request.getBusinessCode(),
                request.getShortCode(), request.getExpireTime(), request.isPermanent())) {
            String text = part == null ? null : part.toString();
            value.append(text == null ? -1 : text.length()).append(':');
            if (text != null) value.append(text);
        }
        return ApiSignProtocol.sha256(value.toString());
    }

    private void validate(Object request) {
        if (request == null || !validator.validate(request).isEmpty()) throw new CommonException("请求参数校验失败");
    }

    public static Long tenant() {
        Long id = RequestContextHolder.getTenantId();
        if (id == null) throw new CommonException("缺少可信租户上下文");
        return id;
    }
}
