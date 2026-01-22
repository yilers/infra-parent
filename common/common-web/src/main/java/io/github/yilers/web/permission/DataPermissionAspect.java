package io.github.yilers.web.permission;

import cn.hutool.v7.core.text.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Aspect
@Component
@Slf4j
public class DataPermissionAspect {

    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final ConcurrentMap<MethodSignature, Method> METHOD_SIGNATURE_CACHE = new ConcurrentHashMap<>();

    @Pointcut("@annotation(com.jzy.permission.DataPermission) || @annotation(com.jzy.permission.DataScope)")
    public void pointcut() {}

    @Around("pointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        Method method = getMethod(point);

        List<DataPermissionContext> ctxList = new ArrayList<>();

        DataPermission dataPermission = method.getAnnotation(DataPermission.class);
        if (dataPermission == null) {
            dataPermission = method.getDeclaringClass().getAnnotation(DataPermission.class);
        }
        if (dataPermission != null) {
            addPermissionContext(dataPermission, method, ctxList);
        }

        DataScope dataScope = method.getAnnotation(DataScope.class);
        if (dataScope == null) {
            dataScope = method.getDeclaringClass().getAnnotation(DataScope.class);
        }
        if (dataScope != null) {
            for (DataPermission permission : dataScope.value()) {
                addPermissionContext(permission, method, ctxList);
            }
        }

        try {
            DataPermissionContextHolder.set(ctxList);
            return point.proceed();
        } finally {
            DataPermissionContextHolder.clear();
        }
    }

    private void addPermissionContext(DataPermission permission, Method method, List<DataPermissionContext> ctxList) {
        DataPermissionContext context = new DataPermissionContext();
        context.setTableName(permission.tableName());
        context.setColumnName(permission.columnName());
        context.setColumnValue(parseValue(permission.columnValue(), method));
        context.setDeptField(permission.deptField());
        context.setUserField(permission.userField());
        ctxList.add(context);
    }

    private String parseValue(String spel, Method method) {
        EvaluationContext context = new StandardEvaluationContext();
        if (StrUtil.isNotBlank(spel)) {
            return PARSER.parseExpression(spel).getValue(context, String.class);
        } else {
            return "";
        }
    }

    private Method getMethod(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return METHOD_SIGNATURE_CACHE.computeIfAbsent(signature, sig -> {
            try {
                return joinPoint.getTarget().getClass()
                        .getMethod(sig.getName(), sig.getParameterTypes());
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
