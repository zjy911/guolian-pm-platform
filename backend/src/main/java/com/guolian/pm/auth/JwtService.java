package com.guolian.pm.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import com.guolian.pm.common.config.AppProperties;
import com.guolian.pm.common.exception.BizException;
import com.guolian.pm.common.web.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * 自实现 JWT（不接 SSO）。Token 携带 currentRoleId、currentOrgId、impersonate 与只读标志。
 * 前端不落 localStorage，仅存内存 + 刷新页时通过 cookie 之外的 sessionStorage 兜底由前端决定。
 */
@Component
@RequiredArgsConstructor
public class JwtService {

    private final AppProperties properties;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(LoginUser user) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(properties.getJwt().getAccessTokenMinutes() * 60L);
        var builder = Jwts.builder()
                .subject(String.valueOf(user.getUserId()))
                .claim("uname", user.getUsername())
                .claim("rid", user.getRoleId())
                .claim("oid", user.getOrgId())
                .claim("sid", user.getSessionId())
                .claim("imp", user.isImpersonate())
                .claim("ro", user.isReadOnly())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key());
        if (user.isImpersonate()) {
            builder.claim("by", user.getImpersonateBy());
            long impExp = user.getImpersonateExpire() == null
                    ? exp.getEpochSecond()
                    : user.getImpersonateExpire().atZone(ZoneId.systemDefault()).toEpochSecond();
            builder.claim("impExp", impExp);
        }
        return builder.compact();
    }

    /**
     * 解析 Token 并还原身份骨架；权限与数据范围由 RolePermissionService 按 roleId 加载。
     */
    public LoginUser parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key()).build()
                    .parseSignedClaims(token).getPayload();
            LoginUser user = new LoginUser();
            user.setUserId(Long.valueOf(claims.getSubject()));
            user.setUsername(claims.get("uname", String.class));
            user.setRoleId(toLong(claims.get("rid")));
            user.setOrgId(toLong(claims.get("oid")));
            user.setSessionId(claims.get("sid", String.class));
            user.setImpersonate(Boolean.TRUE.equals(claims.get("imp")));
            user.setReadOnly(Boolean.TRUE.equals(claims.get("ro")));
            if (user.isImpersonate()) {
                user.setImpersonateBy(claims.get("by", String.class));
                Long impExp = toLong(claims.get("impExp"));
                if (impExp != null) {
                    user.setImpersonateExpire(LocalDateTime.ofInstant(Instant.ofEpochSecond(impExp),
                            ZoneId.systemDefault()));
                    if (user.getImpersonateExpire().isBefore(LocalDateTime.now())) {
                        throw BizException.unauthorized("模拟查看已超过 2 小时，已自动退出");
                    }
                }
            }
            return user;
        } catch (JwtException | IllegalArgumentException ex) {
            throw BizException.unauthorized("登录已失效，请重新登录");
        }
    }

    private Long toLong(Object v) {
        if (v == null) {
            return null;
        }
        return Long.valueOf(String.valueOf(v));
    }
}
