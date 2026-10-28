package com.guolian.pm.common.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guolian.pm.common.web.AuthContext;
import com.guolian.pm.common.web.LoginUser;
import com.guolian.pm.system.log.entity.SysOperLog;
import com.guolian.pm.system.log.mapper.SysOperLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.List;

/**
 * 操作日志切面。参数中的密码、令牌一律替换为 ***，绝不落库。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OpLogAspect {

    private final SysOperLogMapper operLogMapper;
    private final ObjectMapper objectMapper;

    @Around("@annotation(opLog)")
    public Object around(ProceedingJoinPoint point, OpLog opLog) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = null;
        Throwable error = null;
        try {
            result = point.proceed();
            return result;
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            try {
                record(point, opLog, result, error, System.currentTimeMillis() - start);
            } catch (Exception e) {
                log.warn("操作日志写入失败：{}", e.getMessage());
            }
        }
    }

    private void record(ProceedingJoinPoint point, OpLog opLog, Object result, Throwable error, long cost) {
        SysOperLog entity = new SysOperLog();
        entity.setModule(opLog.module());
        entity.setAction(opLog.action());
        MethodSignature signature = (MethodSignature) point.getSignature();
        entity.setMethod(signature.getDeclaringType().getSimpleName() + "." + signature.getName());
        entity.setParams(sanitize(point.getArgs()));
        entity.setResult(error == null ? truncate(toJson(result)) : null);
        entity.setCostMs(cost);
        entity.setSuccess(error == null ? 1 : 0);
        if (error != null) {
            entity.setErrorMsg(truncate(error.getMessage()));
        }
        LoginUser user = AuthContext.get();
        if (user != null) {
            entity.setOperatorId(user.getUserId());
            entity.setOperatorName(user.getRealName());
            entity.setRoleName(user.getRoleName() + "（" + user.getOrgName() + "）");
        }
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            entity.setRequestUri(request.getRequestURI());
            entity.setIp(clientIp(request));
        }
        operLogMapper.insert(entity);
    }

    private String sanitize(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        List<Object> plain = new ArrayList<>();
        for (Object arg : args) {
            if (arg instanceof HttpServletRequest || arg instanceof HttpServletResponse) {
                continue;
            }
            plain.add(arg);
        }
        String json = toJson(plain);
        json = json.replaceAll("(?i)(\"(?:password|oldPassword|newPassword|token|secret)\"\\s*:\\s*)\"[^\"]*\"", "$1\"***\"");
        return truncate(json);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private String truncate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() > 2000 ? text.substring(0, 2000) : text;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
