package com.guolian.pm.pms.product.controller;

import com.guolian.pm.auth.Perm;
import com.guolian.pm.common.api.ApiResponse;
import com.guolian.pm.common.api.PageQuery;
import com.guolian.pm.common.api.PageResult;
import com.guolian.pm.pms.common.dto.PmDtos;
import com.guolian.pm.pms.product.entity.PmProduct;
import com.guolian.pm.pms.product.entity.PmTaskTemplate;
import com.guolian.pm.pms.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 产品管理（设计态）：产品 + 任务模板配置（PRD 5.1）。
 */
@RestController
@RequestMapping("/api/v1/project/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Perm("project:product:list")
    public ApiResponse<PageResult<PmProduct>> page(PageQuery query,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String status) {
        return ApiResponse.ok(productService.page(query, keyword, status));
    }

    @GetMapping("/{id}")
    @Perm("project:product:list")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        PmProduct product = productService.detail(id);
        PmTaskTemplate template = productService.latestTemplate(id);
        return ApiResponse.ok(Map.of("product", product, "template", template));
    }

    @PostMapping
    @Perm("project:product:list")
    public ApiResponse<Map<String, Long>> create(@Valid @RequestBody PmDtos.ProductSaveRequest request) {
        Long id = productService.save(request);
        return ApiResponse.ok(Map.of("id", id));
    }

    @PutMapping("/{id}")
    @Perm("project:product:list")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody PmDtos.ProductSaveRequest request) {
        request.setId(id);
        productService.save(request);
        return ApiResponse.ok(null);
    }

    /** 发布任务模板（版本快照只增不改） */
    @PostMapping("/{id}/template/publish")
    @Perm("project:product:list")
    public ApiResponse<Map<String, Integer>> publishTemplate(@PathVariable Long id,
                                                             @RequestBody Map<String, String> body) {
        Integer version = productService.publishTemplate(id, body.get("templateJson"));
        return ApiResponse.ok(Map.of("version", version));
    }
}
