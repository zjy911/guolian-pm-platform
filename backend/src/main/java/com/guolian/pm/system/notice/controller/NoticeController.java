package com.guolian.pm.system.notice.controller;

import com.guolian.pm.auth.Perm;
import com.guolian.pm.common.api.ApiResponse;
import com.guolian.pm.common.api.PageQuery;
import com.guolian.pm.common.api.PageResult;
import com.guolian.pm.common.log.OpLog;
import com.guolian.pm.common.web.AuthContext;
import com.guolian.pm.system.notice.entity.SysNotice;
import com.guolian.pm.system.notice.service.BadgeService;
import com.guolian.pm.system.notice.service.NoticeService;
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
@RequestMapping("/api/v1/system/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;
    private final BadgeService badgeService;

    @GetMapping
    @Perm("system:notice:list")
    public ApiResponse<PageResult<SysNotice>> page(PageQuery query,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) Integer noticeType,
                                                   @RequestParam(required = false) Integer status) {
        return ApiResponse.ok(noticeService.page(query, keyword, noticeType, status));
    }

    /** 顶栏铃铛用：我的通知公告 */
    @GetMapping("/mine")
    public ApiResponse<List<SysNotice>> mine(@RequestParam(required = false) Integer noticeType) {
        return ApiResponse.ok(noticeService.myNotices(noticeType));
    }

    @GetMapping("/todos")
    public ApiResponse<List<SysNotice>> todos() {
        return ApiResponse.ok(noticeService.myTodos());
    }

    @GetMapping("/badge")
    public ApiResponse<Map<String, Long>> badge() {
        BadgeService.Badge badge = badgeService.badge(AuthContext.require());
        return ApiResponse.ok(Map.of("todoCount", badge.todoCount(), "unreadCount", badge.unreadCount()));
    }

    @PostMapping("/{id}/read")
    public ApiResponse<Void> read(@PathVariable Long id) {
        noticeService.read(id);
        return ApiResponse.ok();
    }

    @PostMapping("/read-all")
    public ApiResponse<Void> readAll() {
        noticeService.readAll();
        return ApiResponse.ok();
    }

    @PostMapping("/todos/{id}/complete")
    public ApiResponse<Void> completeTodo(@PathVariable Long id) {
        noticeService.completeTodo(id);
        return ApiResponse.ok();
    }

    @PostMapping
    @Perm("system:notice:add")
    @OpLog(module = "通知公告", action = "新增公告")
    public ApiResponse<Map<String, Long>> create(@RequestBody SysNotice notice) {
        notice.setId(null);
        return ApiResponse.ok(Map.of("id", noticeService.save(notice)));
    }

    @PutMapping("/{id}")
    @Perm("system:notice:edit")
    @OpLog(module = "通知公告", action = "修改公告")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody SysNotice notice) {
        notice.setId(id);
        noticeService.save(notice);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    @Perm("system:notice:remove")
    @OpLog(module = "通知公告", action = "删除公告")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        noticeService.delete(id);
        return ApiResponse.ok();
    }
}
