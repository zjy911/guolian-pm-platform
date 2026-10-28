package com.guolian.pm.system.sensitive.service;

import java.math.BigDecimal;

/**
 * 脱敏算法。所有算法均为纯函数，便于单测与导出复用。
 */
public final class MaskUtil {

    private MaskUtil() {
    }

    public static String apply(String rule, String param, String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return switch (rule == null ? "FULL_MASK" : rule) {
            case "KEEP_HEAD_TAIL" -> keepHeadTail(value, param);
            case "KEEP_TAIL" -> keepTail(value, param);
            case "EMAIL_MASK" -> email(value);
            case "ADDRESS_MASK" -> address(value);
            case "AMOUNT_RANGE" -> amountRange(value, param);
            default -> "***";
        };
    }

    /** 138****8888 */
    private static String keepHeadTail(String value, String param) {
        int head = 3;
        int tail = 4;
        if (param != null && param.contains(",")) {
            String[] parts = param.split(",");
            try {
                head = Integer.parseInt(parts[0].trim());
                tail = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException ignored) {
                // 保持默认值
            }
        }
        if (value.length() <= head + tail) {
            return "*".repeat(Math.max(1, value.length() - 1)) + value.substring(Math.max(0, value.length() - 1));
        }
        String mask = "*".repeat(value.length() - head - tail);
        return value.substring(0, head) + mask + value.substring(value.length() - tail);
    }

    /** **** **** **** 1234 */
    private static String keepTail(String value, String param) {
        int tail = 4;
        if (param != null) {
            try {
                tail = Integer.parseInt(param.trim());
            } catch (NumberFormatException ignored) {
                // 保持默认值
            }
        }
        String digits = value.replace(" ", "");
        if (digits.length() <= tail) {
            return "*".repeat(Math.max(1, digits.length() - 1)) + digits.substring(Math.max(0, digits.length() - 1));
        }
        String masked = "*".repeat(digits.length() - tail) + digits.substring(digits.length() - tail);
        if (masked.length() >= 12) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < masked.length(); i++) {
                if (i > 0 && i % 4 == 0) {
                    sb.append(' ');
                }
                sb.append(masked.charAt(i));
            }
            return sb.toString();
        }
        return masked;
    }

    /** z***@guolian.com */
    private static String email(String value) {
        int at = value.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        String local = value.substring(0, at);
        String head = local.substring(0, 1);
        return head + "***" + value.substring(at);
    }

    /** 保留省市，其余掩码 */
    private static String address(String value) {
        int provinceIdx = indexOfAny(value, "省", "市", "区", "县");
        if (provinceIdx > 0 && provinceIdx < value.length() - 1) {
            return value.substring(0, provinceIdx + 1) + "***";
        }
        return value.length() > 6 ? value.substring(0, 6) + "***" : "***";
    }

    /** 区间显示：≥1,000 */
    private static String amountRange(String value, String param) {
        try {
            BigDecimal amount = new BigDecimal(value);
            BigDecimal step = param == null ? BigDecimal.valueOf(1000) : new BigDecimal(param.trim());
            BigDecimal floor = amount.divide(step, 0, java.math.RoundingMode.DOWN).multiply(step);
            return "≥" + String.format("%,.0f", floor.doubleValue());
        } catch (NumberFormatException e) {
            return "***";
        }
    }

    private static int indexOfAny(String value, String... tokens) {
        int min = -1;
        for (String token : tokens) {
            int idx = value.indexOf(token);
            if (idx > 0 && (min < 0 || idx < min)) {
                min = idx;
            }
        }
        return min;
    }
}
