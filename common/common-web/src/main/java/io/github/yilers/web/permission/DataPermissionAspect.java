package io.github.yilers.web.permission;

import cn.hutool.v7.core.text.StrUtil;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据权限上下文拦截器。
 * 使用静态方法匹配的Advisor识别实现类及接口方法上的注解，避免仅使用@annotation切点遗漏接口声明。
 * SpEL统一返回ID集合，SQL条件由数据权限Handler结合表名和别名生成。
 */
@Component
public class DataPermissionAspect extends StaticMethodMatcherPointcutAdvisor {

    public DataPermissionAspect() {
        setAdvice((MethodInterceptor) this::around);
    }

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        Method specificMethod = AopUtils.getMostSpecificMethod(method, targetClass);
        return permission(specificMethod, method) != null || scope(specificMethod, method) != null;
    }

    private Object around(MethodInvocation invocation) throws Throwable {
        Method declaredMethod = invocation.getMethod();
        Method method = AopUtils.getMostSpecificMethod(declaredMethod, invocation.getThis().getClass());
        Method evaluationMethod = method.getAnnotation(DataPermission.class) != null
                || method.getAnnotation(DataScope.class) != null ? method : declaredMethod;

        List<DataPermissionContext> ctxList = new ArrayList<>();

        DataPermission dataPermission = permission(method, declaredMethod);
        if (dataPermission != null) {
            addPermissionContext(dataPermission, evaluationMethod, invocation.getArguments(), ctxList);
        }

        DataScope dataScope = scope(method, declaredMethod);
        if (dataScope != null) {
            for (DataPermission permission : dataScope.value()) {
                addPermissionContext(permission, evaluationMethod, invocation.getArguments(), ctxList);
            }
        }

        List<DataPermissionContext> previous = DataPermissionContextHolder.get();
        try {
            DataPermissionContextHolder.set(ctxList);
            return invocation.proceed();
        } finally {
            // 内层方法退出后恢复外层范围；由最外层调用负责最终清理。
            if (previous == null) {
                DataPermissionContextHolder.clear();
            } else {
                DataPermissionContextHolder.set(previous);
            }
        }
    }

    private DataPermission permission(Method method, Method declaredMethod) {
        DataPermission annotation = AnnotatedElementUtils.findMergedAnnotation(method, DataPermission.class);
        return annotation != null ? annotation : AnnotatedElementUtils.findMergedAnnotation(declaredMethod, DataPermission.class);
    }

    private DataScope scope(Method method, Method declaredMethod) {
        DataScope annotation = AnnotatedElementUtils.findMergedAnnotation(method, DataScope.class);
        return annotation != null ? annotation : AnnotatedElementUtils.findMergedAnnotation(declaredMethod, DataScope.class);
    }

    private void addPermissionContext(DataPermission permission, Method method, Object[] arguments,
                                      List<DataPermissionContext> ctxList) {
        DataPermissionContext context = new DataPermissionContext();
        context.setTableName(permission.tableName());
        context.setColumnName(permission.columnName());
        context.setColumnValue(permission.columnValue());
        if (StrUtil.isNotBlank(permission.columnValue())) {
            context.setColumnValues(DataPermissionSpelResolver.resolve(permission.columnValue(), method, arguments));
        }
        context.setDeptField(permission.deptField());
        context.setUserField(permission.userField());
        ctxList.add(context);
    }

}
