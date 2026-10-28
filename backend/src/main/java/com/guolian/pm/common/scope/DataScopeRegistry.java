package com.guolian.pm.common.scope;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据权限表注册表：表名 → 归属人字段名。
 * 底座内置：sys_user（按角色绑定的组织反查）、sys_account_transfer、sys_notice。
 * 业务模块上线时注册自己的表，主流程代码无需修改。
 */
public class DataScopeRegistry {

    private static final Map<String, String> TABLE_OWNER_COLUMN = new ConcurrentHashMap<>();

    static {
        // 用户表没有 org_id，按「其角色所绑定的组织」反查，owner 字段即主键
        TABLE_OWNER_COLUMN.put("sys_user", "id");
        TABLE_OWNER_COLUMN.put("sys_account_transfer", "from_user_id");
        // 通知/公告按 receiver_id 显式控制可见性（存在全员公告），不走组织范围
    }

    private DataScopeRegistry() {
    }

    public static void register(String tableName, String ownerColumn) {
        TABLE_OWNER_COLUMN.put(tableName.toLowerCase(), ownerColumn);
    }

    public static String ownerColumn(String tableName) {
        return TABLE_OWNER_COLUMN.get(tableName.toLowerCase());
    }

    public static Map<String, String> all() {
        return Map.copyOf(TABLE_OWNER_COLUMN);
    }
}
