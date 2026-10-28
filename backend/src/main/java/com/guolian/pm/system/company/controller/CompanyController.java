package com.guolian.pm.system.company.controller;

import com.guolian.pm.auth.Perm;
import com.guolian.pm.common.api.ApiResponse;
import com.guolian.pm.common.api.PageQuery;
import com.guolian.pm.common.api.PageResult;
import com.guolian.pm.common.log.OpLog;
import com.guolian.pm.system.company.entity.SysCompany;
import com.guolian.pm.system.company.service.CompanyService;
import com.guolian.pm.system.company.vo.CompanyVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping
    @Perm("system:company:list")
    public ApiResponse<PageResult<CompanyVO>> page(PageQuery query,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) Integer status) {
        return ApiResponse.ok(companyService.page(query, keyword, status));
    }

    /** 下拉用：启用中的主体 */
    @GetMapping("/options")
    public ApiResponse<List<CompanyVO>> options() {
        return ApiResponse.ok(companyService.options());
    }

    @PostMapping
    @Perm("system:company:add")
    @OpLog(module = "公司主体", action = "新增主体")
    public ApiResponse<Map<String, Long>> create(@RequestBody SysCompany request) {
        request.setId(null);
        return ApiResponse.ok(Map.of("id", companyService.save(request)));
    }

    @PutMapping("/{id}")
    @Perm("system:company:edit")
    @OpLog(module = "公司主体", action = "修改主体")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody SysCompany request) {
        request.setId(id);
        companyService.save(request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    @Perm("system:company:remove")
    @OpLog(module = "公司主体", action = "删除主体")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        companyService.delete(id);
        return ApiResponse.ok();
    }
}
