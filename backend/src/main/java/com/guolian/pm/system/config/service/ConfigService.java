package com.guolian.pm.system.config.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guolian.pm.system.config.entity.SysConfig;
import com.guolian.pm.system.config.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 参数读取：业务代码通过它取参数，避免任何硬编码阈值。
 */
@Service
@RequiredArgsConstructor
public class ConfigService {

    private final SysConfigMapper configMapper;

    public String get(String key, String defaultValue) {
        SysConfig config = configMapper.selectOne(Wrappers.<SysConfig>lambdaQuery()
                .eq(SysConfig::getConfigKey, key));
        return config == null || config.getConfigValue() == null ? defaultValue : config.getConfigValue();
    }

    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key, String.valueOf(defaultValue));
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    public java.util.List<SysConfig> list(String keyword) {
        var wrapper = Wrappers.<SysConfig>lambdaQuery();
        if (org.springframework.util.StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysConfig::getConfigKey, keyword)
                    .or().like(SysConfig::getConfigName, keyword));
        }
        wrapper.orderByAsc(SysConfig::getConfigKey);
        return configMapper.selectList(wrapper);
    }

    public void updateValue(Long id, String value, String remark) {
        SysConfig config = configMapper.selectById(id);
        if (config == null) {
            throw com.guolian.pm.common.exception.BizException.notFound("参数不存在");
        }
        config.setConfigValue(value);
        if (remark != null) {
            config.setRemark(remark);
        }
        configMapper.updateById(config);
    }

    public Long create(SysConfig config) {
        Long exists = configMapper.selectCount(Wrappers.<SysConfig>lambdaQuery()
                .eq(SysConfig::getConfigKey, config.getConfigKey()));
        if (exists != null && exists > 0) {
            throw com.guolian.pm.common.exception.BizException.biz("参数键「" + config.getConfigKey() + "」已存在");
        }
        configMapper.insert(config);
        return config.getId();
    }

    public void delete(Long id) {
        SysConfig config = configMapper.selectById(id);
        if (config != null && Integer.valueOf(1).equals(config.getBuiltin())) {
            throw com.guolian.pm.common.exception.BizException.biz("内置参数不允许删除");
        }
        configMapper.deleteById(id);
    }
}
