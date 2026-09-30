package io.github.yilers.upm.handler;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.v7.crypto.SecureUtil;
import cn.hutool.v7.crypto.digest.BCrypt;
import io.github.yilers.core.constant.CommonConst;
import io.github.yilers.core.enums.UserTypeEnum;
import io.github.yilers.upm.entity.Dept;
import io.github.yilers.upm.entity.Position;
import io.github.yilers.upm.entity.Role;
import io.github.yilers.upm.entity.User;
import io.github.yilers.upm.entity.UserRole;
import io.github.yilers.upm.request.UserBatchAddRequest;
import io.github.yilers.upm.request.UserBatchRowRequest;
import io.github.yilers.upm.response.UserBatchAddResponse;
import io.github.yilers.upm.response.UserBatchRowResponse;
import io.github.yilers.upm.service.DeptService;
import io.github.yilers.upm.service.PositionService;
import io.github.yilers.upm.service.RoleService;
import io.github.yilers.upm.service.UserRoleService;
import io.github.yilers.upm.service.UserService;
import io.github.yilers.web.exception.CommonException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 批量新增用户处理器。
 *
 * <p>批量入口不会循环调用单用户新增方法。账号、关联数据和部门数据权限会先批量加载，
 * 校验通过的用户再统一写入，避免批量录入产生大量重复查询。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserBatchHandler {

    private static final int MAX_ACCOUNT_LENGTH = 50;
    private static final int MAX_NAME_LENGTH = 50;
    private static final int MAX_EMAIL_LENGTH = 30;
    private static final int MAX_PHONE_LENGTH = 20;
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^.\\s@]+(?:\\.[^.\\s@]+)+$");

    private final UserService userService;
    private final UserRoleService userRoleService;
    private final DeptService deptService;
    private final PositionService positionService;
    private final RoleService roleService;
    private final CommonHandler commonHandler;

    /**
     * 校验并新增批量用户。业务校验失败的行不会入库，其他有效行在同一事务中写入。
     *
     * @param request 批量用户数据
     * @return 每一行对应的处理结果
     */
    @Transactional(rollbackFor = Exception.class)
    public UserBatchAddResponse add(UserBatchAddRequest request) {
        List<UserBatchRowRequest> requests = request.getUsers();
        List<BatchRow> rows = createRows(requests);
        BatchReference reference = loadReference(rows);
        validate(rows, reference);

        List<BatchRow> validRows = rows.stream().filter(BatchRow::valid).toList();
        if (!validRows.isEmpty()) {
            save(validRows);
        }
        return buildResponse(rows);
    }

    private List<BatchRow> createRows(List<UserBatchRowRequest> requests) {
        List<BatchRow> rows = new ArrayList<>(requests.size());
        for (int index = 0; index < requests.size(); index++) {
            UserBatchRowRequest request = requests.get(index);
            UserBatchRowResponse response = new UserBatchRowResponse();
            response.setRowKey(hasText(request.getRowKey()) ? request.getRowKey().trim() : String.valueOf(index + 1));
            rows.add(new BatchRow(request, response, trim(request.getAccount()), trim(request.getName()),
                    trim(request.getNickname()), trim(request.getPhone()), trim(request.getEmail())));
        }
        return rows;
    }

    private BatchReference loadReference(List<BatchRow> rows) {
        Set<String> accounts = rows.stream().map(BatchRow::account)
                .filter(UserBatchHandler::hasText).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> existingAccounts = userService.findByAccountList(new ArrayList<>(accounts)).stream()
                .map(User::getAccount).collect(Collectors.toSet());

        Set<Long> deptIds = collectIds(rows, row -> row.request().getDeptId());
        Set<Long> positionIds = collectIds(rows, row -> row.request().getPositionId());
        Set<Long> roleIds = rows.stream()
                .map(row -> row.request().getRoleIdList())
                .filter(list -> list != null && !list.isEmpty())
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        Map<Long, Dept> depts = mapById(
                deptIds.isEmpty() ? List.of() : deptService.listByIds(deptIds), Dept::getId);
        Map<Long, Position> positions = mapById(
                positionIds.isEmpty() ? List.of() : positionService.listByIds(positionIds), Position::getId);
        Map<Long, Role> roles = mapById(roleIds.isEmpty() ? List.of() : roleService.listByIds(roleIds), Role::getId);
        List<Long> dataScope = commonHandler.findDataScopeByUserId(StpUtil.getLoginIdAsLong(), null);

        return new BatchReference(existingAccounts, depts, positions, roles,
                dataScope == null ? null : new HashSet<>(dataScope));
    }

    private void validate(List<BatchRow> rows, BatchReference reference) {
        Map<String, Long> accountCounts = rows.stream()
                .map(BatchRow::account)
                .filter(UserBatchHandler::hasText)
                .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));

        for (BatchRow row : rows) {
            validateBase(row);
            if (hasText(row.account()) && accountCounts.getOrDefault(row.account(), 0L) > 1) {
                row.addError("本批次账号重复");
            }
            if (reference.existingAccounts().contains(row.account())) {
                row.addError("账号已存在");
            }
            validateDept(row, reference);
            validatePosition(row, reference);
            validateRoles(row, reference);
        }
    }

    private void validateBase(BatchRow row) {
        if (!hasText(row.account())) {
            row.addError("用户账号不能为空");
        } else if (row.account().length() > MAX_ACCOUNT_LENGTH) {
            row.addError("用户账号不能超过50个字符");
        }
        if (!hasText(row.name())) {
            row.addError("姓名不能为空");
        } else if (row.name().length() > MAX_NAME_LENGTH) {
            row.addError("姓名不能超过50个字符");
        }
        if (hasText(row.nickname()) && row.nickname().length() > MAX_NAME_LENGTH) {
            row.addError("昵称不能超过50个字符");
        }
        if (hasText(row.phone())) {
            if (row.phone().length() > MAX_PHONE_LENGTH || !PHONE_PATTERN.matcher(row.phone()).matches()) {
                row.addError("手机号码格式不正确");
            }
        }
        if (hasText(row.email())) {
            if (row.email().length() > MAX_EMAIL_LENGTH || !EMAIL_PATTERN.matcher(row.email()).matches()) {
                row.addError("邮箱格式不正确");
            }
        }
        Integer gender = row.request().getGender();
        if (gender != null && !CommonConst.YES.equals(gender) && !CommonConst.NO.equals(gender)) {
            row.addError("性别只能为男或女");
        }
    }

    private void validateDept(BatchRow row, BatchReference reference) {
        Long deptId = row.request().getDeptId();
        if (deptId == null) {
            row.addError("所属部门不能为空");
            return;
        }
        Dept dept = reference.depts().get(deptId);
        if (dept == null) {
            row.addError("所属部门不存在");
        } else if (!CommonConst.YES.equals(dept.getUsable())) {
            row.addError("所属部门已停用");
        }
        Set<Long> allowedDeptIds = reference.allowedDeptIds();
        if (allowedDeptIds != null && !allowedDeptIds.contains(deptId)) {
            row.addError("无权向该部门添加用户");
        }
    }

    private void validatePosition(BatchRow row, BatchReference reference) {
        Long positionId = row.request().getPositionId();
        if (positionId == null) {
            return;
        }
        Position position = reference.positions().get(positionId);
        if (position == null) {
            row.addError("职位不存在");
        } else if (!CommonConst.YES.equals(position.getUsable())) {
            row.addError("职位已停用");
        }
    }

    private void validateRoles(BatchRow row, BatchReference reference) {
        List<Long> roleIdList = row.request().getRoleIdList();
        if (roleIdList == null || roleIdList.isEmpty()) {
            return;
        }
        for (Long roleId : new LinkedHashSet<>(roleIdList)) {
            Role role = reference.roles().get(roleId);
            if (role == null) {
                row.addError("角色不存在");
            } else if (!CommonConst.YES.equals(role.getUsable())) {
                row.addError("角色已停用");
            }
        }
    }

    private void save(List<BatchRow> validRows) {
        // 初始密码对所有批量用户相同，每批生成一次带随机盐的BCrypt结果，避免500次计算造成接口超时。
        String password = BCrypt.hashpw(SecureUtil.md5(CommonConst.INIT_PWD));
        List<User> users = new ArrayList<>(validRows.size());
        for (BatchRow row : validRows) {
            User user = new User();
            user.setUserType(UserTypeEnum.ADMIN.getCode());
            user.setAccount(row.account());
            user.setName(row.name());
            user.setNickname(hasText(row.nickname()) ? row.nickname() : row.name());
            user.setPassword(password);
            user.setDeptId(row.request().getDeptId());
            user.setPositionId(row.request().getPositionId());
            user.setPhone(row.phone());
            user.setEmail(row.email());
            user.setGender(row.request().getGender());
            user.setOperable(CommonConst.YES);
            user.setUsable(CommonConst.YES);
            user.setDeleted(CommonConst.NO);
            user.setVersion(1);
            user.setExpand(UserExpandHelper.setInitPwd(null, true));
            users.add(user);
        }

        if (!userService.saveBatch(users, 100)) {
            throw new CommonException("批量新增用户失败");
        }

        List<UserRole> relations = new ArrayList<>();
        for (int index = 0; index < validRows.size(); index++) {
            BatchRow row = validRows.get(index);
            User user = users.get(index);
            row.response().setSuccess(true);
            row.response().setUserId(user.getId());
            List<Long> roleIdList = row.request().getRoleIdList();
            if (roleIdList == null) {
                continue;
            }
            for (Long roleId : new LinkedHashSet<>(roleIdList)) {
                UserRole relation = new UserRole();
                relation.setUserId(user.getId());
                relation.setRoleId(roleId);
                relations.add(relation);
            }
        }
        if (!relations.isEmpty() && !userRoleService.saveBatch(relations, 200)) {
            throw new CommonException("批量保存用户角色失败");
        }
        log.info("批量新增用户完成，成功数量:{}", validRows.size());
    }

    private UserBatchAddResponse buildResponse(List<BatchRow> rows) {
        UserBatchAddResponse response = new UserBatchAddResponse();
        List<UserBatchRowResponse> rowResponses = rows.stream().map(BatchRow::response).toList();
        int successCount = (int) rowResponses.stream().filter(UserBatchRowResponse::isSuccess).count();
        response.setTotalCount(rows.size());
        response.setSuccessCount(successCount);
        response.setFailureCount(rows.size() - successCount);
        response.setRows(rowResponses);
        return response;
    }

    private static Set<Long> collectIds(List<BatchRow> rows, Function<BatchRow, Long> getter) {
        return rows.stream().map(getter).filter(id -> id != null).collect(Collectors.toSet());
    }

    private static <T> Map<Long, T> mapById(Collection<T> values, Function<T, Long> idGetter) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyMap();
        }
        return values.stream()
                .collect(Collectors.toMap(idGetter, Function.identity(), (first, ignored) -> first));
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record BatchReference(Set<String> existingAccounts,
                                  Map<Long, Dept> depts,
                                  Map<Long, Position> positions,
                                  Map<Long, Role> roles,
                                  Set<Long> allowedDeptIds) {
    }

    private record BatchRow(UserBatchRowRequest request,
                            UserBatchRowResponse response,
                            String account,
                            String name,
                            String nickname,
                            String phone,
                            String email) {

        private boolean valid() {
            return response.getMessages().isEmpty();
        }

        private void addError(String message) {
            if (!response.getMessages().contains(message)) {
                response.getMessages().add(message);
            }
        }
    }
}
