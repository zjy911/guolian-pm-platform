package com.guolian.pm.crm.common.controller;

import com.guolian.pm.auth.Perm;
import com.guolian.pm.common.api.ApiResponse;
import com.guolian.pm.common.log.OpLog;
import com.guolian.pm.crm.common.dto.CrmDtos;
import com.guolian.pm.crm.common.entity.CrmImportBatch;
import com.guolian.pm.crm.common.service.CrmDupCheckService;
import com.guolian.pm.crm.common.service.CrmImportService;
import com.guolian.pm.crm.common.service.CrmStatsService;
import com.guolian.pm.crm.common.service.ProductRefService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CRM 公共接口：查重、导入模板/导入/进度、统计与看板供数、产品引用代理。
 */
@RestController
@RequestMapping("/api/v1/crm/common")
@RequiredArgsConstructor
public class CrmCommonController {

    private final CrmDupCheckService dupCheckService;
    private final CrmImportService importService;
    private final CrmStatsService statsService;
    private final ProductRefService productRefService;

    /** 查重：接口与导入共用同一套实现 */
    @GetMapping("/check-duplicate")
    @Perm("crm:lead:list")
    public ApiResponse<CrmDtos.DupCheckResult> checkDuplicate(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String mobile,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String creditCode,
            @RequestParam(required = false) String companyName,
            @RequestParam(required = false) String province) {
        return ApiResponse.ok(dupCheckService.check(type, mobile, email, creditCode, companyName, province));
    }

    /** 下载导入模板 */
    @GetMapping("/import-template")
    @Perm("crm:lead:import")
    public ResponseEntity<byte[]> template(@RequestParam String bizType) {
        byte[] bytes = importService.template(bizType);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"crm-" + bizType.toLowerCase() + "-template.xlsx\"")
                .body(bytes);
    }

    /** 执行导入（整批校验，全部通过才落库） */
    @PostMapping("/import")
    @Perm("crm:lead:import")
    @OpLog(module = "CRM-导入", action = "执行导入")
    public ApiResponse<Map<String, Object>> importFile(@RequestParam String bizType,
                                                       @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(importService.importFile(bizType, file));
    }

    /** 导入批次结果与错误报告 */
    @GetMapping("/imports/{batchId}")
    @Perm("crm:lead:import")
    public ApiResponse<CrmImportBatch> batch(@PathVariable Long batchId) {
        return ApiResponse.ok(importService.batch(batchId));
    }

    /** 首页四张小卡 */
    @GetMapping("/stats/summary")
    @Perm("crm:lead:list")
    public ApiResponse<Map<String, Object>> summary() {
        return ApiResponse.ok(statsService.summary());
    }

    /** 最小看板：漏斗与转化率 */
    @GetMapping("/stats/funnel")
    @Perm("crm:opportunity:list")
    public ApiResponse<Map<String, Object>> funnel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.ok(statsService.funnel(startDate, endDate));
    }

    /** 产品引用代理：项目管理模块未接入时返回 degraded=true */
    @GetMapping("/products")
    public ApiResponse<Map<String, Object>> products() {
        Map<String, Object> result = new LinkedHashMap<>();
        boolean available = productRefService.isAvailable();
        List<ProductRefService.ProductBrief> list = productRefService.listEnabled();
        result.put("degraded", !available);
        result.put("list", list);
        if (!available) {
            result.put("message", "项目管理模块未接入，服务产品暂不可选");
        }
        return ApiResponse.ok(result);
    }
}
