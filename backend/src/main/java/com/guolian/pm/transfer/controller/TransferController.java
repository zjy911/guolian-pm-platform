package com.guolian.pm.transfer.controller;

import com.guolian.pm.auth.Perm;
import com.guolian.pm.common.api.ApiResponse;
import com.guolian.pm.common.api.PageQuery;
import com.guolian.pm.common.api.PageResult;
import com.guolian.pm.common.log.OpLog;
import com.guolian.pm.common.web.AuthContext;
import com.guolian.pm.transfer.dto.TransferDtos;
import com.guolian.pm.transfer.service.TransferService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 账号交接：本期唯一正式跑通的审批场景。
 */
@RestController
@RequestMapping("/api/v1/system/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @GetMapping
    @Perm("system:transfer:list")
    public ApiResponse<PageResult<TransferDtos.DetailVO>> page(
            PageQuery query,
            @RequestParam(required = false) String flowStatus,
            @RequestParam(required = false) String execStatus,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(transferService.page(query, flowStatus, execStatus, keyword));
    }

    @GetMapping("/{id}")
    @Perm("system:transfer:list")
    public ApiResponse<TransferDtos.DetailVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(transferService.detail(id));
    }

    @GetMapping("/{id}/items")
    @Perm("system:transfer:list")
    public ApiResponse<List<TransferDtos.ItemVO>> items(@PathVariable Long id) {
        return ApiResponse.ok(transferService.items(id));
    }

    @GetMapping("/adapters")
    @Perm("system:transfer:list")
    public ApiResponse<List<TransferDtos.AdapterInfo>> adapters() {
        return ApiResponse.ok(transferService.adapters());
    }

    @GetMapping("/precheck")
    @Perm("system:transfer:list")
    public ApiResponse<TransferDtos.PrecheckVO> precheck(@RequestParam Long fromUserId) {
        return ApiResponse.ok(transferService.precheck(fromUserId));
    }

    @GetMapping("/eligible-receivers")
    @Perm("system:transfer:list")
    public ApiResponse<List<TransferDtos.UserBrief>> receivers(@RequestParam Long fromUserId,
                                                               @RequestParam Long roleId) {
        return ApiResponse.ok(transferService.eligibleReceivers(fromUserId, roleId));
    }

    @PostMapping
    @Perm("system:transfer:add")
    @OpLog(module = "账号交接", action = "新增交接单")
    public ApiResponse<Map<String, Long>> create(@Valid @RequestBody TransferDtos.CreateRequest request) {
        return ApiResponse.ok(Map.of("id", transferService.create(request, AuthContext.require())));
    }

    @PostMapping("/{id}/submit")
    @Perm("system:transfer:submit")
    @OpLog(module = "账号交接", action = "提交交接审批")
    public ApiResponse<Map<String, String>> submit(@PathVariable Long id) {
        String instanceId = transferService.submit(id, AuthContext.require());
        return ApiResponse.ok(Map.of("processInstanceId", instanceId));
    }

    @PostMapping("/{id}/execute")
    @Perm("system:transfer:execute")
    @OpLog(module = "账号交接", action = "执行交接")
    public ApiResponse<Void> execute(@PathVariable Long id) {
        transferService.execute(id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/retry")
    @Perm("system:transfer:execute")
    @OpLog(module = "账号交接", action = "重试失败项")
    public ApiResponse<Void> retry(@PathVariable Long id) {
        transferService.retry(id);
        return ApiResponse.ok();
    }

    @Data
    public static class RevokeRequest {
        private String reason;
    }

    @PostMapping("/{id}/revoke")
    @Perm("system:transfer:revoke")
    @OpLog(module = "账号交接", action = "撤销交接")
    public ApiResponse<Void> revoke(@PathVariable Long id, @RequestBody(required = false) RevokeRequest request) {
        transferService.revoke(id, request == null ? null : request.getReason());
        return ApiResponse.ok();
    }
}
