package com.guolian.pm.crm.common.service;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 产品引用的降级实现：项目管理模块未接入时返回空列表并标记不可用。
 *
 * <p>项目管理模块上线后，由该模块提供 {@code @Primary} 的实现覆盖本类，
 * CRM 侧代码（商机 / 合同 / 协议的下拉与校验）无需修改。
 */
@Service
public class DegradedProductRefService implements ProductRefService {

    @Override
    public ProductBrief getById(Long productId) {
        return null;
    }

    @Override
    public List<ProductBrief> listEnabled() {
        return List.of();
    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}
