package com.guolian.pm.system.dict.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guolian.pm.common.api.PageQuery;
import com.guolian.pm.common.api.PageResult;
import com.guolian.pm.common.cache.CacheService;
import com.guolian.pm.common.exception.BizException;
import com.guolian.pm.system.dict.entity.SysDictItem;
import com.guolian.pm.system.dict.entity.SysDictType;
import com.guolian.pm.system.dict.mapper.SysDictItemMapper;
import com.guolian.pm.system.dict.mapper.SysDictTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 字典管理：所有下拉统一取字典，前端做本地缓存。
 */
@Service
@RequiredArgsConstructor
public class DictService {

    private static final String CACHE_PREFIX = "dict:";
    private static final long CACHE_TTL = 300L;

    private final SysDictTypeMapper dictTypeMapper;
    private final SysDictItemMapper dictItemMapper;
    private final CacheService cache;

    public PageResult<SysDictType> pageTypes(PageQuery query, String keyword) {
        LambdaQueryWrapper<SysDictType> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysDictType::getDictName, keyword).or().like(SysDictType::getDictType, keyword));
        }
        wrapper.orderByAsc(SysDictType::getDictType);
        Page<SysDictType> page = dictTypeMapper.selectPage(
                new Page<>(query.safePageNum(), query.safePageSize()), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal(), query.safePageNum(), query.safePageSize());
    }

    public List<SysDictType> allTypes() {
        return dictTypeMapper.selectList(Wrappers.<SysDictType>lambdaQuery()
                .eq(SysDictType::getStatus, 1).orderByAsc(SysDictType::getDictType));
    }

    public List<SysDictItem> items(String dictType) {
        return dictItemMapper.selectList(Wrappers.<SysDictItem>lambdaQuery()
                .eq(SysDictItem::getDictType, dictType)
                .eq(SysDictItem::getStatus, 1)
                .orderByAsc(SysDictItem::getSortNo));
    }

    public Map<String, List<SysDictItem>> data(String types) {
        Map<String, List<SysDictItem>> result = new LinkedHashMap<>();
        if (!StringUtils.hasText(types)) {
            return result;
        }
        for (String type : Arrays.asList(types.split(","))) {
            String key = type.trim();
            if (key.isEmpty()) {
                continue;
            }
            @SuppressWarnings("unchecked")
            List<SysDictItem> cached = cache.get(CACHE_PREFIX + key, List.class);
            if (cached == null) {
                cached = items(key);
                cache.put(CACHE_PREFIX + key, cached, CACHE_TTL);
            }
            result.put(key, cached == null ? new ArrayList<>() : cached);
        }
        return result;
    }

    @Transactional
    public Long saveType(SysDictType type) {
        if (!StringUtils.hasText(type.getDictType())) {
            throw BizException.badRequest("请输入字典类型编码");
        }
        Long exists = dictTypeMapper.selectCount(Wrappers.<SysDictType>lambdaQuery()
                .eq(SysDictType::getDictType, type.getDictType())
                .ne(type.getId() != null, SysDictType::getId, type.getId()));
        if (exists != null && exists > 0) {
            throw BizException.biz("字典类型「" + type.getDictType() + "」已存在");
        }
        if (type.getId() == null) {
            dictTypeMapper.insert(type);
        } else {
            dictTypeMapper.updateById(type);
        }
        cache.evict(CACHE_PREFIX + type.getDictType());
        return type.getId();
    }

    @Transactional
    public void deleteType(Long id) {
        SysDictType type = dictTypeMapper.selectById(id);
        if (type == null) {
            return;
        }
        dictTypeMapper.deleteById(id);
        dictItemMapper.delete(Wrappers.<SysDictItem>lambdaQuery().eq(SysDictItem::getDictType, type.getDictType()));
        cache.evict(CACHE_PREFIX + type.getDictType());
    }

    @Transactional
    public Long saveItem(SysDictItem item) {
        if (!StringUtils.hasText(item.getDictType()) || !StringUtils.hasText(item.getDictValue())) {
            throw BizException.badRequest("字典类型与字典值不能为空");
        }
        if (item.getId() == null) {
            dictItemMapper.insert(item);
        } else {
            dictItemMapper.updateById(item);
        }
        cache.evict(CACHE_PREFIX + item.getDictType());
        return item.getId();
    }

    @Transactional
    public void deleteItem(Long id) {
        SysDictItem item = dictItemMapper.selectById(id);
        if (item == null) {
            return;
        }
        dictItemMapper.deleteById(id);
        cache.evict(CACHE_PREFIX + item.getDictType());
    }
}
