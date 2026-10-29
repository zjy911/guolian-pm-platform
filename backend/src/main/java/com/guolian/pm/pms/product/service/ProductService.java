package com.guolian.pm.pms.product.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guolian.pm.common.api.PageQuery;
import com.guolian.pm.common.api.PageResult;
import com.guolian.pm.common.exception.BizException;
import com.guolian.pm.crm.common.service.CrmOwnerSupport;
import com.guolian.pm.pms.common.dto.PmDtos;
import com.guolian.pm.pms.product.entity.PmProduct;
import com.guolian.pm.pms.product.entity.PmTaskTemplate;
import com.guolian.pm.pms.product.mapper.PmProductMapper;
import com.guolian.pm.pms.product.mapper.PmTaskTemplateMapper;
import com.guolian.pm.system.code.service.CodeRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 产品与任务模板（设计态）。产品定型 → 模板发布 → 项目实例化取快照（PRD 4.3）。
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final PmProductMapper productMapper;
    private final PmTaskTemplateMapper templateMapper;
    private final CrmOwnerSupport owner;
    private final CodeRuleService codeRuleService;

    public PageResult<PmProduct> page(PageQuery query, String keyword, String status) {
        var wrapper = Wrappers.<PmProduct>lambdaQuery()
                .and(StringUtils.hasText(keyword), w -> w.like(PmProduct::getProductName, keyword)
                        .or().like(PmProduct::getProductCode, keyword))
                .eq(StringUtils.hasText(status), PmProduct::getStatus, status)
                .orderByDesc(PmProduct::getCreatedTime);
        Page<PmProduct> page = productMapper.selectPage(new Page<>(query.safePageNum(), query.safePageSize()), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal(), query.safePageNum(), query.safePageSize());
    }

    public PmProduct detail(Long id) {
        PmProduct product = productMapper.selectById(id);
        if (product == null) {
            throw BizException.notFound("产品不存在");
        }
        return product;
    }

    @Transactional
    public Long save(PmDtos.ProductSaveRequest request) {
        PmProduct product = new PmProduct();
        boolean create = request.getId() == null;
        if (!create) {
            product = detail(request.getId());
        }
        product.setProductCode(request.getProductCode());
        product.setProductName(request.getProductName());
        product.setBgOrgId(request.getBgOrgId());
        product.setBgOrgName(request.getBgOrgName());
        product.setPricingMode(request.getPricingMode());
        product.setRemark(request.getRemark());
        // 编码唯一校验
        Long dup = productMapper.selectCount(Wrappers.<PmProduct>lambdaQuery()
                .eq(PmProduct::getProductCode, request.getProductCode())
                .ne(!create, PmProduct::getId, request.getId()));
        if (dup != null && dup > 0) {
            throw BizException.biz("产品编码「" + request.getProductCode() + "」已存在");
        }
        if (create) {
            product.setStatus("ON_SALE");
            product.setTemplateVersion("v0（未发布）");
            product.setOwnerUserId(owner.currentUserId());
            product.setOwnerOrgId(owner.createOrgId());
            productMapper.insert(product);
        } else {
            productMapper.updateById(product);
        }
        return product.getId();
    }

    /** 发布模板：版本 +1 落快照，产品指向新版本；已立项项目不受影响（PRD 4.3） */
    @Transactional
    public Integer publishTemplate(Long productId, String templateJson) {
        PmProduct product = detail(productId);
        if (!StringUtils.hasText(templateJson)) {
            throw BizException.biz("模板内容不能为空");
        }
        PmTaskTemplate latest = latestTemplate(productId);
        int nextVersion = latest == null ? 1 : latest.getVersion() + 1;
        PmTaskTemplate template = new PmTaskTemplate();
        template.setProductId(productId);
        template.setVersion(nextVersion);
        template.setTemplateJson(templateJson);
        template.setStatus("PUBLISHED");
        template.setPublishTime(LocalDateTime.now());
        templateMapper.insert(template);

        PmProduct update = new PmProduct();
        update.setId(productId);
        update.setTemplateVersion("v" + nextVersion + ".0");
        productMapper.updateById(update);
        return nextVersion;
    }

    public PmTaskTemplate latestTemplate(Long productId) {
        List<PmTaskTemplate> list = templateMapper.selectList(Wrappers.<PmTaskTemplate>lambdaQuery()
                .eq(PmTaskTemplate::getProductId, productId)
                .eq(PmTaskTemplate::getStatus, "PUBLISHED")
                .orderByDesc(PmTaskTemplate::getVersion)
                .last("limit 1"));
        return list.isEmpty() ? null : list.get(0);
    }

    /** 项目实例化时读取指定版本快照（优先精确版本，找不到回落最新发布版） */
    public PmTaskTemplate templateFor(PmProduct product, String version) {
        if (StringUtils.hasText(version)) {
            List<PmTaskTemplate> exact = templateMapper.selectList(Wrappers.<PmTaskTemplate>lambdaQuery()
                    .eq(PmTaskTemplate::getProductId, product.getId())
                    .eq(PmTaskTemplate::getStatus, "PUBLISHED")
                    .orderByDesc(PmTaskTemplate::getVersion).last("limit 1"));
            if (!exact.isEmpty()) {
                return exact.get(0);
            }
        }
        return latestTemplate(product.getId());
    }
}
