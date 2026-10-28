package com.guolian.pm.system.log.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guolian.pm.auth.Perm;
import com.guolian.pm.common.api.ApiResponse;
import com.guolian.pm.common.api.PageQuery;
import com.guolian.pm.common.api.PageResult;
import com.guolian.pm.system.log.entity.SysLoginLog;
import com.guolian.pm.system.log.entity.SysOperLog;
import com.guolian.pm.system.log.mapper.SysLoginLogMapper;
import com.guolian.pm.system.log.mapper.SysOperLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 操作日志与登录日志查询。两者本身也按当前角色策略处理手机号等信息。
 */
@RestController
@RequestMapping("/api/v1/system/logs")
@RequiredArgsConstructor
public class LogController {

    private final SysOperLogMapper operLogMapper;
    private final SysLoginLogMapper loginLogMapper;

    @GetMapping("/oper")
    @Perm("system:log:oper")
    public ApiResponse<PageResult<SysOperLog>> operLogs(
            PageQuery query,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) Integer success,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        LambdaQueryWrapper<SysOperLog> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysOperLog::getOperatorName, keyword)
                    .or().like(SysOperLog::getAction, keyword));
        }
        if (StringUtils.hasText(module)) {
            wrapper.eq(SysOperLog::getModule, module);
        }
        if (success != null) {
            wrapper.eq(SysOperLog::getSuccess, success);
        }
        if (startTime != null) {
            wrapper.ge(SysOperLog::getCreatedTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(SysOperLog::getCreatedTime, endTime);
        }
        wrapper.orderByDesc(SysOperLog::getCreatedTime);
        Page<SysOperLog> page = operLogMapper.selectPage(
                new Page<>(query.safePageNum(), query.safePageSize()), wrapper);
        return ApiResponse.ok(PageResult.of(page.getRecords(), page.getTotal(),
                query.safePageNum(), query.safePageSize()));
    }

    @GetMapping("/login")
    @Perm("system:log:login")
    public ApiResponse<PageResult<SysLoginLog>> loginLogs(
            PageQuery query,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String loginType,
            @RequestParam(required = false) Integer success) {
        LambdaQueryWrapper<SysLoginLog> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(SysLoginLog::getUsername, keyword);
        }
        if (StringUtils.hasText(loginType)) {
            wrapper.eq(SysLoginLog::getLoginType, loginType);
        }
        if (success != null) {
            wrapper.eq(SysLoginLog::getSuccess, success);
        }
        wrapper.orderByDesc(SysLoginLog::getCreatedTime);
        Page<SysLoginLog> page = loginLogMapper.selectPage(
                new Page<>(query.safePageNum(), query.safePageSize()), wrapper);
        return ApiResponse.ok(PageResult.of(page.getRecords(), page.getTotal(),
                query.safePageNum(), query.safePageSize()));
    }
}
