package com.github.yilers.upm.log;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.v7.extra.spring.SpringUtil;
import cn.hutool.v7.http.server.servlet.ServletUtil;
import cn.hutool.v7.json.JSONArray;
import cn.hutool.v7.json.JSONObject;
import cn.hutool.v7.json.JSONUtil;
import com.github.yilers.core.constant.CommonConst;
import com.github.yilers.upm.controller.AuthController;
import com.github.yilers.upm.entity.Log;
import com.github.yilers.upm.entity.User;
import com.github.yilers.upm.request.LoginRequest;
import com.github.yilers.upm.service.UserService;
import com.github.yilers.web.log.SysLog;
import com.github.yilers.web.util.Ip2RegionUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 系统日志切面处理类
 *
 * @author hui.zhang
 * @date 2021/1/28 11:04 上午
 */

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class SysLogAspect {
    private final ThreadPoolExecutor commonExecutor;
    private static final Map<Method, SysLog> SYS_LOG_CACHE = new ConcurrentHashMap<>();

    /**
     * 切点
     */
    @Pointcut("@annotation(com.jzy.log.SysLog)")
    public void logPointCut() {
    }

    /**
     * 环绕通知
     * @param joinPoint
     */
    @Around("logPointCut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        return saveLog(joinPoint);
    }

    /**
     * 保存日志
     * @param joinPoint
     */
    private Object saveLog(ProceedingJoinPoint joinPoint) throws Throwable {
        Log sysLog = new Log();
        long startTime = System.currentTimeMillis();  // 开始时间
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String className = joinPoint.getTarget().getClass().getName();
            String methodName = signature.getName();
            Object[] args = joinPoint.getArgs();
            String params = JSONUtil.toJsonStr(args);

            Method method = signature.getMethod();
            SysLog syslog = SYS_LOG_CACHE.computeIfAbsent(method, m -> m.getAnnotation(SysLog.class));

            String action = syslog.value();
            String module = syslog.module();
            String[] hideFieldList = syslog.hideFieldList();

            // 如果需要隐藏指定字段
            if (hideFieldList.length > 0) {
                params = hideJsonFields(params, hideFieldList);
            }
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
            String ip = ServletUtil.getClientIP(request);
            sysLog.setIp(ip);
            sysLog.setRegion(Ip2RegionUtil.region(ip));
            sysLog.setAction(action);
            sysLog.setModule(module);
            sysLog.setMethod(className + "." + methodName + "()");
            sysLog.setParams(params);
            sysLog.setDeleted(CommonConst.NO);
            sysLog.setCreateTime(LocalDateTime.now());
            try {
                String userId = StpUtil.getLoginIdAsString();
                UserService userService = SpringUtil.getBean(UserService.class);
                User user = userService.findById(Long.parseLong(userId));
                sysLog.setOperator(userId);
                if (user != null) {
                    sysLog.setDeptId(user.getDeptId());
                }
            } catch (Exception e) {
                log.warn("获取不到用户信息");
                // 如果是登录接口com.jzy.controller.AuthController
                if (AuthController.class.getName().equals(className) && "login".equals(methodName)) {
                    // [{"account":"admin@jzy.com","password":"***","device":"web"}]
                    LoginRequest loginRequest = (LoginRequest) args[0];
                    String account = loginRequest.getAccount();
                    UserService userService = SpringUtil.getBean(UserService.class);
                    User user = userService.findByAccount(account);
                    if (user != null) {
                        sysLog.setOperator(user.getId().toString());
                        sysLog.setDeptId(user.getDeptId());
                    }
                }
            }

            Object proceed = joinPoint.proceed();
            sysLog.setSuccess(CommonConst.YES);
            String detail = String.format("(%s)访问:%s.%s() 传入:%s 执行:%s",
                    ip, className, methodName, params, action);
            log.info(JSONUtil.toJsonStr(detail));
            return proceed;
        } catch (Exception e) {
            sysLog.setSuccess(CommonConst.NO);
            sysLog.setStackTrace(e.getMessage());
            throw e;
        } finally {
            long cost = System.currentTimeMillis() - startTime;
            sysLog.setDuration((int) cost);
            log.info("方法执行耗时:{}ms", cost);
            try {
                commonExecutor.execute(sysLog::insert);
            } catch (Exception e) {
                log.error("保存日志失败", e);
            }
        }
    }

    private String hideJsonFields(String json, String[] hideFieldList) {
        try {
            Object jsonObj = JSONUtil.parse(json);
            if (jsonObj instanceof JSONUtil) {
                // 是数组，逐个处理
                JSONArray array = (JSONArray) jsonObj;
                for (int i = 0; i < array.size(); i++) {
                    hideFieldsInJsonObject(array.getJSONObject(i), hideFieldList);
                }
            } else if (jsonObj instanceof JSONObject) {
                hideFieldsInJsonObject((JSONObject) jsonObj, hideFieldList);
            }
            return jsonObj.toString();
        } catch (Exception e) {
            log.warn("参数脱敏失败", e);
            return json;
        }
    }

    private void hideFieldsInJsonObject(JSONObject jsonObject, String[] fieldNames) {
        for (String field : fieldNames) {
            if (jsonObject.containsKey(field)) {
                jsonObject.putValue(field, "***");
            }
        }
    }

}
