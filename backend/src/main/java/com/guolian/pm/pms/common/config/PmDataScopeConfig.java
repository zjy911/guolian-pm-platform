package com.guolian.pm.pms.common.config;

import com.guolian.pm.common.scope.DataScopeRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

/**
 * 项目管理模块数据权限注册：主表均为「按组织归属」语义（项目经理为 owner_user_id）。
 */
@Configuration
public class PmDataScopeConfig {

    @PostConstruct
    public void register() {
        DataScopeRegistry.registerOrgScoped("pm_project", "owner_user_id", "owner_org_id");
        DataScopeRegistry.registerOrgScoped("pm_product", "owner_user_id", "owner_org_id");
    }
}
