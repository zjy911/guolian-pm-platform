package com.guolian.pm.common.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 集中式配置。所有值来自环境变量，启动时统一校验，缺失即快速失败。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Cors cors = new Cors();
    private Cache cache = new Cache();
    private Security security = new Security();
    private Debug debug = new Debug();
    private Transfer transfer = new Transfer();
    private Upload upload = new Upload();

    @Data
    public static class Jwt {
        private String secret;
        private int accessTokenMinutes = 120;
        private int impersonateMaxHours = 2;
    }

    @Data
    public static class Cors {
        private String allowedOrigins = "http://localhost:5173";
    }

    @Data
    public static class Cache {
        private String type = "memory";
        private long ttlSeconds = 300;
    }

    @Data
    public static class Security {
        private int loginFailLockCount = 5;
        private int loginFailLockMinutes = 10;
        private String initialPassword = "Init@123456";
    }

    @Data
    public static class Debug {
        private boolean impersonateEnabled = false;
    }

    @Data
    public static class Transfer {
        private int revokeDays = 7;
        private int batchSize = 200;
        private int maxItemsPerType = 5000;
    }

    @Data
    public static class Upload {
        private String dir = "./data/uploads";
    }

    /**
     * 启动即校验关键配置，避免带病启动。
     */
    @PostConstruct
    public void validate() {
        if (!StringUtils.hasText(jwt.getSecret()) || jwt.getSecret().length() < 32) {
            throw new IllegalStateException(
                    "配置缺失：app.jwt.secret 必须通过环境变量 JWT_SECRET 注入，且长度不少于 32 位");
        }
        if (jwt.getAccessTokenMinutes() <= 0) {
            throw new IllegalStateException("配置错误：app.jwt.access-token-minutes 必须大于 0");
        }
        if (!StringUtils.hasText(cors.getAllowedOrigins())) {
            throw new IllegalStateException("配置缺失：app.cors.allowed-origins 必须显式指定来源，禁止使用通配符");
        }
        if (cors.getAllowedOrigins().contains("*")) {
            throw new IllegalStateException("配置错误：CORS 不允许使用通配符 *，必须显式列出前端来源");
        }
    }
}
