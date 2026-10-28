package com.guolian.pm.system.company.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guolian.pm.common.api.PageQuery;
import com.guolian.pm.common.api.PageResult;
import com.guolian.pm.common.exception.BizException;
import com.guolian.pm.system.company.entity.SysCompany;
import com.guolian.pm.system.company.mapper.SysCompanyMapper;
import com.guolian.pm.system.company.vo.CompanyVO;
import com.guolian.pm.system.user.entity.SysUser;
import com.guolian.pm.system.user.entity.SysUserCompany;
import com.guolian.pm.system.user.mapper.SysUserCompanyMapper;
import com.guolian.pm.system.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 公司主体。与组织无直接关系，只与用户关联；任何组织接口都不返回主体信息。
 */
@Service
@RequiredArgsConstructor
public class CompanyService {

    private final SysCompanyMapper companyMapper;
    private final SysUserCompanyMapper userCompanyMapper;
    private final SysUserMapper userMapper;

    public PageResult<CompanyVO> page(PageQuery query, String keyword, Integer status) {
        LambdaQueryWrapper<SysCompany> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysCompany::getCompanyName, keyword)
                    .or().like(SysCompany::getCompanyCode, keyword)
                    .or().like(SysCompany::getShortName, keyword));
        }
        if (status != null) {
            wrapper.eq(SysCompany::getStatus, status);
        }
        wrapper.orderByAsc(SysCompany::getCompanyCode);
        Page<SysCompany> page = companyMapper.selectPage(
                new Page<>(query.safePageNum(), query.safePageSize()), wrapper);
        return PageResult.of(toVOList(page.getRecords()), page.getTotal(),
                query.safePageNum(), query.safePageSize());
    }

    public List<CompanyVO> options() {
        return toVOList(companyMapper.selectList(Wrappers.<SysCompany>lambdaQuery()
                .eq(SysCompany::getStatus, 1)
                .orderByAsc(SysCompany::getCompanyCode)));
    }

    @Transactional
    public Long save(SysCompany request) {
        if (request.getId() == null) {
            Long codeExists = companyMapper.selectCount(Wrappers.<SysCompany>lambdaQuery()
                    .eq(SysCompany::getCompanyCode, request.getCompanyCode()));
            if (codeExists != null && codeExists > 0) {
                throw BizException.biz("主体编码「" + request.getCompanyCode() + "」已存在");
            }
        }
        if (StringUtils.hasText(request.getCreditCode())) {
            if (request.getCreditCode().length() != 18) {
                throw BizException.badRequest("统一社会信用代码需为 18 位");
            }
            Long creditExists = companyMapper.selectCount(Wrappers.<SysCompany>lambdaQuery()
                    .eq(SysCompany::getCreditCode, request.getCreditCode())
                    .ne(request.getId() != null, SysCompany::getId, request.getId()));
            if (creditExists != null && creditExists > 0) {
                throw BizException.biz("统一社会信用代码已存在");
            }
        }
        if (request.getId() == null) {
            companyMapper.insert(request);
        } else {
            companyMapper.updateById(request);
        }
        return request.getId();
    }

    @Transactional
    public void delete(Long id) {
        Long activeUsers = countUsers(id);
        if (activeUsers > 0) {
            throw BizException.biz("该主体下仍有 " + activeUsers + " 名用户在岗，仅允许停用");
        }
        companyMapper.deleteById(id);
    }

    private long countUsers(Long companyId) {
        List<SysUserCompany> relations = userCompanyMapper.selectList(Wrappers.<SysUserCompany>lambdaQuery()
                .eq(SysUserCompany::getCompanyId, companyId)
                .eq(SysUserCompany::getRelationType, 1));
        if (relations.isEmpty()) {
            return 0;
        }
        Set<Long> userIds = new HashSet<>();
        relations.forEach(r -> userIds.add(r.getUserId()));
        return userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .in(SysUser::getId, userIds)
                .ne(SysUser::getStatus, 3));
    }

    private List<CompanyVO> toVOList(List<SysCompany> companies) {
        List<CompanyVO> result = new ArrayList<>();
        if (companies.isEmpty()) {
            return result;
        }
        Map<Long, Long> counts = new HashMap<>();
        for (SysUserCompany relation : userCompanyMapper.selectList(Wrappers.<SysUserCompany>lambdaQuery()
                .eq(SysUserCompany::getRelationType, 1))) {
            counts.merge(relation.getCompanyId(), 1L, Long::sum);
        }
        for (SysCompany company : companies) {
            CompanyVO vo = new CompanyVO();
            vo.setId(company.getId());
            vo.setCompanyCode(company.getCompanyCode());
            vo.setCompanyName(company.getCompanyName());
            vo.setShortName(company.getShortName());
            vo.setCompanyType(company.getCompanyType());
            vo.setParentCompanyId(company.getParentCompanyId());
            vo.setLegalPerson(company.getLegalPerson());
            vo.setCreditCode(company.getCreditCode());
            vo.setRegAddress(company.getRegAddress());
            vo.setOfficeAddress(company.getOfficeAddress());
            vo.setTaxNo(company.getTaxNo());
            vo.setBankName(company.getBankName());
            vo.setBankAccount(company.getBankAccount());
            vo.setStatus(company.getStatus());
            vo.setUserCount(counts.getOrDefault(company.getId(), 0L));
            result.add(vo);
        }
        return result;
    }
}
