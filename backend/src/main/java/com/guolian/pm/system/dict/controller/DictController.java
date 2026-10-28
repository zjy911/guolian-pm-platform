package com.guolian.pm.system.dict.controller;

import com.guolian.pm.auth.Perm;
import com.guolian.pm.common.api.ApiResponse;
import com.guolian.pm.common.api.PageQuery;
import com.guolian.pm.common.api.PageResult;
import com.guolian.pm.common.log.OpLog;
import com.guolian.pm.system.dict.entity.SysDictItem;
import com.guolian.pm.system.dict.entity.SysDictType;
import com.guolian.pm.system.dict.service.DictService;
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
@RequestMapping("/api/v1/system/dicts")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;

    @GetMapping("/types")
    @Perm("system:dict:list")
    public ApiResponse<PageResult<SysDictType>> pageTypes(PageQuery query,
                                                          @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(dictService.pageTypes(query, keyword));
    }

    @GetMapping("/types/all")
    public ApiResponse<List<SysDictType>> allTypes() {
        return ApiResponse.ok(dictService.allTypes());
    }

    @PostMapping("/types")
    @Perm("system:dict:add")
    @OpLog(module = "字典管理", action = "新增字典类型")
    public ApiResponse<Map<String, Long>> createType(@RequestBody SysDictType type) {
        type.setId(null);
        return ApiResponse.ok(Map.of("id", dictService.saveType(type)));
    }

    @PutMapping("/types/{id}")
    @Perm("system:dict:edit")
    @OpLog(module = "字典管理", action = "修改字典类型")
    public ApiResponse<Void> updateType(@PathVariable Long id, @RequestBody SysDictType type) {
        type.setId(id);
        dictService.saveType(type);
        return ApiResponse.ok();
    }

    @DeleteMapping("/types/{id}")
    @Perm("system:dict:remove")
    @OpLog(module = "字典管理", action = "删除字典类型")
    public ApiResponse<Void> deleteType(@PathVariable Long id) {
        dictService.deleteType(id);
        return ApiResponse.ok();
    }

    @GetMapping("/types/{dictType}/items")
    public ApiResponse<List<SysDictItem>> items(@PathVariable String dictType) {
        return ApiResponse.ok(dictService.items(dictType));
    }

    @PostMapping("/items")
    @Perm("system:dict:add")
    @OpLog(module = "字典管理", action = "新增字典项")
    public ApiResponse<Map<String, Long>> createItem(@RequestBody SysDictItem item) {
        item.setId(null);
        return ApiResponse.ok(Map.of("id", dictService.saveItem(item)));
    }

    @PutMapping("/items/{id}")
    @Perm("system:dict:edit")
    @OpLog(module = "字典管理", action = "修改字典项")
    public ApiResponse<Void> updateItem(@PathVariable Long id, @RequestBody SysDictItem item) {
        item.setId(id);
        dictService.saveItem(item);
        return ApiResponse.ok();
    }

    @DeleteMapping("/items/{id}")
    @Perm("system:dict:remove")
    @OpLog(module = "字典管理", action = "删除字典项")
    public ApiResponse<Void> deleteItem(@PathVariable Long id) {
        dictService.deleteItem(id);
        return ApiResponse.ok();
    }

    /** 前端 useDict 统一入口 */
    @GetMapping("/data")
    public ApiResponse<Map<String, List<SysDictItem>>> data(@RequestParam String types) {
        return ApiResponse.ok(dictService.data(types));
    }
}
