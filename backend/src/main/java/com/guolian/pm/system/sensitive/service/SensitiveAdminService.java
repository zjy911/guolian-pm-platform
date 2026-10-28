package com.guolian.pm.system.sensitive.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guolian.pm.common.config.AppProperties;
import com.guolian.pm.common.exception.BizException;
import com.guolian.pm.common.util.CryptoUtil;
import com.guolian.pm.common.web.AuthContext;
import com.guolian.pm.common.web.LoginUser;
import com.guolian.pm.system.company.entity.SysCompany;
import com.guolian.pm.system.company.mapper.SysCompanyMapper;
import com.guolian.pm.system.sensitive.entity.SysSensitiveField;
import com.guolian.pm.system.sensitive.entity.SysSensitiveGroup;
import com.guolian.pm.system.sensitive.entity.SysSensitiveLog;
import com.guolian.pm.system.sensitive.mapper.SysSensitiveFieldMapper;
import com.guolian.pm.system.sensitive.mapper.SysSensitiveGroupMapper;
import com.guolian.pm.system.sensitive.mapper.SysSensitiveLogMapper;
import com.guolian.pm.system.user.entity.SysUser;
import com.guolian.pm.system.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 脱敏配置管理与「查看明文」审计。
 */
@Service
@RequiredArgsConstructor
public class SensitiveAdminService {

    private final SysSensitiveGroupMapper groupMapper;
    private final SysSensitiveFieldMapper fieldMapper;
    private final SysSensitiveLogMapper sensitiveLogMapper;
    private final SysUserMapper userMapper;
    private final SysCompanyMapper companyMapper;
    private final SensitivePolicyService policyService;
    private final AppProperties properties;

    /**
     * 字段白名单：前端为下拉选择，避免超管手填错字段名导致脱敏静默失效。
     */
    public Map<String, List<Map<String, String>>> whitelist() {
        Map<String, List<Map<String, String>>> result = new LinkedHashMap<>();
        result.put("user", List.of(
                field("mobile", "手机号", "MOBILE", "KEEP_HEAD_TAIL", "3,4"),
                field("email", "邮箱", "EMAIL", "EMAIL_MASK", null),
                field("idCard", "身份证号", "ID_CARD", "KEEP_TAIL", "4"),
                field("salaryAmount", "月薪（预留）", "SALARY", "AMOUNT_RANGE", "1000")));
        result.put("company", List.of(
                field("bankAccount", "银行账号", "BANK_ACCOUNT", "KEEP_TAIL", "4"),
                field("regAddress", "注册地址", "ADDRESS", "ADDRESS_MASK", null),
                field("officeAddress", "办公地址", "ADDRESS", "ADDRESS_MASK", null)));
        return result;
    }

    private Map<String, String> field(String name, String label, String group, String rule, String param) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("fieldName", name);
        map.put("fieldLabel", label);
        map.put("groupCode", group);
        map.put("maskRule", rule);
        map.put("maskParam", param);
        return map;
    }

    public List<SysSensitiveGroup> groups() {
        return groupMapper.selectList(Wrappers.<SysSensitiveGroup>lambdaQuery()
                .orderByAsc(SysSensitiveGroup::getSortNo));
    }

    public List<SysSensitiveField> fields(String groupCode, String bizObject) {
        var wrapper = Wrappers.<SysSensitiveField>lambdaQuery();
        if (StringUtils.hasText(groupCode)) {
            wrapper.eq(SysSensitiveField::getGroupCode, groupCode);
        }
        if (StringUtils.hasText(bizObject)) {
            wrapper.eq(SysSensitiveField::getBizObject, bizObject);
        }
        return fieldMapper.selectList(wrapper.orderByAsc(SysSensitiveField::getGroupCode));
    }

    @Transactional
    public Long saveGroup(SysSensitiveGroup group) {
        if (!StringUtils.hasText(group.getGroupCode())) {
            throw BizException.badRequest("请输入字段组编码");
        }
        Long exists = groupMapper.selectCount(Wrappers.<SysSensitiveGroup>lambdaQuery()
                .eq(SysSensitiveGroup::getGroupCode, group.getGroupCode())
                .ne(group.getId() != null, SysSensitiveGroup::getId, group.getId()));
        if (exists != null && exists > 0) {
            throw BizException.biz("字段组编码已存在");
        }
        if (group.getId() == null) {
            groupMapper.insert(group);
        } else {
            groupMapper.updateById(group);
        }
        policyService.evictAll();
        return group.getId();
    }

    @Transactional
    public void deleteGroup(Long id) {
        SysSensitiveGroup group = groupMapper.selectById(id);
        if (group == null) {
            return;
        }
        groupMapper.deleteById(id);
        fieldMapper.delete(Wrappers.<SysSensitiveField>lambdaQuery()
                .eq(SysSensitiveField::getGroupCode, group.getGroupCode()));
        policyService.evictAll();
    }

    @Transactional
    public Long saveField(SysSensitiveField field) {
        validateWhitelist(field);
        if (field.getId() == null) {
            fieldMapper.insert(field);
        } else {
            fieldMapper.updateById(field);
        }
        policyService.evictAll();
        return field.getId();
    }

    @Transactional
    public void deleteField(Long id) {
        fieldMapper.deleteById(id);
        policyService.evictAll();
    }

    /**
     * 查看明文：校验允许明文且非隐藏档位，写审计日志（不记录明文本身）。
     */
    public String plain(String bizType, Long bizId, String group) {
        LoginUser user = AuthContext.require();
        if (policyService.isHidden(group)) {
            throw BizException.forbidden("当前角色对该字段组为隐藏档位，不可查看明文");
        }
        if (!policyService.plainAllowed(group)) {
            throw BizException.forbidden("该字段组为高风险且未开放明文查看");
        }
        String value = readPlainValue(bizType, bizId, group);
        if (value == null) {
            throw BizException.notFound("未找到该字段值");
        }
        SysSensitiveLog logEntity = new SysSensitiveLog();
        logEntity.setUserId(user.getUserId());
        logEntity.setRoleId(user.getRoleId());
        logEntity.setBizType(bizType);
        logEntity.setBizId(bizId);
        logEntity.setGroupCode(group);
        logEntity.setFieldName(resolveFieldName(group));
        sensitiveLogMapper.insert(logEntity);
        return value;
    }

    private String readPlainValue(String bizType, Long bizId, String group) {
        if ("user".equals(bizType)) {
            SysUser user = userMapper.selectById(bizId);
            if (user == null) {
                return null;
            }
            return switch (group) {
                case "MOBILE" -> user.getMobile();
                case "EMAIL" -> user.getEmail();
                case "ID_CARD" -> CryptoUtil.decrypt(properties.getJwt().getSecret(), user.getIdCard());
                default -> null;
            };
        }
        if ("company".equals(bizType)) {
            SysCompany company = companyMapper.selectById(bizId);
            if (company == null) {
                return null;
            }
            return switch (group) {
                case "BANK_ACCOUNT" -> company.getBankAccount();
                case "ADDRESS" -> company.getRegAddress();
                default -> null;
            };
        }
        return null;
    }

    private String resolveFieldName(String group) {
        return switch (group) {
            case "MOBILE" -> "mobile";
            case "EMAIL" -> "email";
            case "ID_CARD" -> "idCard";
            case "BANK_ACCOUNT" -> "bankAccount";
            case "ADDRESS" -> "regAddress";
            default -> group.toLowerCase();
        };
    }

    private void validateWhitelist(SysSensitiveField field) {
        Map<String, List<Map<String, String>>> whitelist = whitelist();
        List<Map<String, String>> candidates = whitelist.getOrDefault(field.getBizObject(), new ArrayList<>());
        boolean allowed = candidates.stream()
                .anyMatch(item -> item.get("fieldName").equals(field.getFieldName()));
        if (!allowed) {
            throw BizException.badRequest("字段名不在白名单内：" + field.getFieldName());
        }
        if (StringUtils.hasText(field.getGroupCode())) {
            SysSensitiveGroup group = groupMapper.selectOne(Wrappers.<SysSensitiveGroup>lambdaQuery()
                    .eq(SysSensitiveGroup::getGroupCode, field.getGroupCode()));
            if (group == null) {
                throw BizException.badRequest("字段组不存在：" + field.getGroupCode());
            }
        }
        if (field.getMaskRule() == null) {
            throw BizException.badRequest("请选择脱敏算法");
        }
    }
}
