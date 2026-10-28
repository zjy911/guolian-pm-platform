package com.guolian.pm.common.scope;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据权限表注册表。
 *
 * <p>两种归属语义，注册时二选一：
 * <ul>
 *   <li><b>按人归属</b>（底座 sys_* 用法）：表里只有「归属人」列，范围通过「归属人所在组织」反查用户集合。
 *       注册方式 {@link #register(String, String)}。</li>
 *   <li><b>按组织归属</b>（业务模块用法，如 CRM）：表里同时有 owner_user_id 与 owner_org_id。
 *       ORG / ORG_AND_CHILD / ORG_CUSTOM 直接对 owner_org_id 过滤，SELF 对 owner_user_id 过滤。
 *       注册方式 {@link #registerOrgScoped(String, String, String)}。</li>
 * </ul>
 *
 * <p>业务模块只需在自己的配置类里注册表，主流程（拦截器）代码无需修改。
 */
public class DataScopeRegistry {

    /** 表名 → 归属人列（按人归属） */
    private static final Map<String, String> TABLE_OWNER_COLUMN = new ConcurrentHashMap<>();

    /** 表名 → 归属组织列（按组织归属） */
    private static final Map<String, String> TABLE_ORG_COLUMN = new ConcurrentHashMap<>();

    /** 表名 → 归属人列（按组织归属时，SELF 范围用） */
    private static final Map<String, String> TABLE_ORG_OWNER_COLUMN = new ConcurrentHashMap<>();

    static {
        // 用户表没有 org_id，按「其角色所绑定的组织」反查，owner 字段即主键
        TABLE_OWNER_COLUMN.put("sys_user", "id");
        TABLE_OWNER_COLUMN.put("sys_account_transfer", "from_user_id");
        // 通知/公告按 receiver_id 显式控制可见性（存在全员公告），不走组织范围
    }

    private DataScopeRegistry() {
    }

    /** 按人归属：只声明归属人列 */
    public static void register(String tableName, String ownerColumn) {
        TABLE_OWNER_COLUMN.put(tableName.toLowerCase(), ownerColumn);
    }

    /**
     * 按组织归属：声明归属人列 + 归属组织列。
     *
     * @param tableName       表名
     * @param ownerUserColumn 归属人列，如 owner_user_id
     * @param ownerOrgColumn  归属组织列，如 owner_org_id
     */
    public static void registerOrgScoped(String tableName, String ownerUserColumn, String ownerOrgColumn) {
        String key = tableName.toLowerCase();
        TABLE_ORG_OWNER_COLUMN.put(key, ownerUserColumn);
        TABLE_ORG_COLUMN.put(key, ownerOrgColumn);
    }

    /** 归属人列（按人归属时使用） */
    public static String ownerColumn(String tableName) {
        return TABLE_OWNER_COLUMN.get(tableName.toLowerCase());
    }

    /** 归属组织列（按组织归属时使用，未注册返回 null） */
    public static String orgColumn(String tableName) {
        return TABLE_ORG_COLUMN.get(tableName.toLowerCase());
    }

    /** 按组织归属表的归属人列 */
    public static String orgScopedOwnerColumn(String tableName) {
        return TABLE_ORG_OWNER_COLUMN.get(tableName.toLowerCase());
    }

    /** 是否已注册（任意一种语义） */
    public static boolean isRegistered(String tableName) {
        String key = tableName.toLowerCase();
        return TABLE_OWNER_COLUMN.containsKey(key) || TABLE_ORG_COLUMN.containsKey(key);
    }

    public static Map<String, String> all() {
        return Map.copyOf(TABLE_OWNER_COLUMN);
    }

    public static Map<String, String> allOrgScoped() {
        return Map.copyOf(TABLE_ORG_COLUMN);
    }
}
