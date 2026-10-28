package com.guolian.pm.crm.common.service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 跨模块产品引用。项目管理模块未接入时必须整体降级，不得在 CRM 建产品表或写死产品列表。
 */
public interface ProductRefService {

    ProductBrief getById(Long productId);

    List<ProductBrief> listEnabled();

    boolean isAvailable();

    record ProductBrief(Long id, String name, String status) {
    }
}
