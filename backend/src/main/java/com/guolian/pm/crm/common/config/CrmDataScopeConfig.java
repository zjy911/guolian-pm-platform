package com.guolian.pm.crm.common.config;

import com.guolian.pm.common.scope.DataScopeRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

/**
 * CRM 数据权限注册：9 张主表全部具备 owner_user_id + owner_org_id。
 *
 * <p>底座拦截器按「归属组织」过滤（ORG / ORG_AND_CHILD / ORG_CUSTOM），
 * SELF 范围自动回落为「负责人 = 当前用户」。注册完即生效，拦截器无需改动。
 *
 * <p>这是业务模块接入底座的范本动作之一：新增业务表后只做「注册」，不写权限 SQL。
 */
@Configuration
public class CrmDataScopeConfig {

    /** 9 张主表：表名 → (归属人列, 归属组织列) */
    private static final String[][] MAIN_TABLES = {
            {"crm_lead", "owner_user_id", "owner_org_id"},
            {"crm_account", "owner_user_id", "owner_org_id"},
            {"crm_contact", "owner_user_id", "owner_org_id"},
            {"crm_opportunity", "owner_user_id", "owner_org_id"},
            {"crm_competitor", "owner_user_id", "owner_org_id"},
            {"crm_activity", "owner_user_id", "owner_org_id"},
            {"crm_contract", "owner_user_id", "owner_org_id"},
            {"crm_agreement", "owner_user_id", "owner_org_id"},
            {"crm_bid", "owner_user_id", "owner_org_id"}
    };

    @PostConstruct
    public void register() {
        for (String[] t : MAIN_TABLES) {
            DataScopeRegistry.registerOrgScoped(t[0], t[1], t[2]);
        }
    }
}
