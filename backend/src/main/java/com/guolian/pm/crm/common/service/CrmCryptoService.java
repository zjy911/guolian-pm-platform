package com.guolian.pm.crm.common.service;

import com.guolian.pm.common.util.CryptoUtil;
import com.guolian.pm.system.config.service.ConfigService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * 手机号 / 邮箱的加密存储与等值查重。
 *
 * <p>方案差异 D8：直接 AES 随机 IV 加密会让「查重」失效，因此双写：
 * 原文加密存 {@code mobile}，另存 {@code mobile_hash = HMAC-SHA256(明文)} 供等值查询。
 * 密钥来自配置中心（参数 {@code crm.crypto.secret}），不落在代码与 SQL 中。
 */
@Component
public class CrmCryptoService {

    private final ConfigService configService;
    private final String fallbackSecret;

    public CrmCryptoService(ConfigService configService,
                            @Value("${crm.crypto.secret:}") String fallbackSecret) {
        this.configService = configService;
        this.fallbackSecret = fallbackSecret;
    }

    private String secret() {
        String s = configService.get("crm.crypto.secret", null);
        if (!StringUtils.hasText(s)) {
            s = fallbackSecret;
        }
        return StringUtils.hasText(s) ? s : "pm-platform-crm-default-secret";
    }

    /** HMAC-SHA256 查重哈希（固定盐，保证同一明文恒定哈希） */
    public String hash(String plain) {
        if (!StringUtils.hasText(plain)) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(plain.trim().getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("查重哈希计算失败", e);
        }
    }

    /** 落库加密（明文不落库） */
    public String encrypt(String plain) {
        if (!StringUtils.hasText(plain)) {
            return plain;
        }
        return CryptoUtil.encrypt(secret(), plain.trim());
    }

    /** 出库解密（仅服务端使用，展示仍走脱敏序列化器） */
    public String decrypt(String stored) {
        if (!StringUtils.hasText(stored)) {
            return stored;
        }
        try {
            return CryptoUtil.decrypt(secret(), stored);
        } catch (Exception e) {
            // 历史明文数据兼容：解密失败按原值返回
            return stored;
        }
    }
}
