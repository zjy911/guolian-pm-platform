package com.guolian.pm.system.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guolian.pm.common.api.PageResult;
import com.guolian.pm.common.cache.CacheService;
import com.guolian.pm.common.config.AppProperties;
import com.guolian.pm.common.exception.BizException;
import com.guolian.pm.common.scope.DataScopeService;
import com.guolian.pm.common.util.CryptoUtil;
import com.guolian.pm.auth.OnlineSessionRegistry;
import com.guolian.pm.auth.RoleContextService;
import com.guolian.pm.system.company.entity.SysCompany;
import com.guolian.pm.system.company.mapper.SysCompanyMapper;
import com.guolian.pm.system.config.service.ConfigService;
import com.guolian.pm.system.role.entity.SysRole;
import com.guolian.pm.system.role.mapper.SysRoleMapper;
import com.guolian.pm.system.sensitive.service.SensitivePolicyService;
import com.guolian.pm.system.user.dto.UserCompanyBrief;
import com.guolian.pm.system.user.dto.UserDtos;
import com.guolian.pm.system.user.dto.UserRoleBrief;
import com.guolian.pm.system.user.entity.SysUser;
import com.guolian.pm.system.user.entity.SysUserCompany;
import com.guolian.pm.system.user.entity.SysUserRole;
import com.guolian.pm.system.user.mapper.SysUserCompanyMapper;
import com.guolian.pm.system.user.mapper.SysUserMapper;
import com.guolian.pm.system.user.mapper.SysUserRoleMapper;
import com.guolian.pm.system.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户管理服务。组织由角色派生，用户表不存组织。
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserCompanyMapper userCompanyMapper;
    private final SysRoleMapper roleMapper;
    private final SysCompanyMapper companyMapper;
    private final DataScopeService dataScopeService;
    private final RoleContextService roleContextService;
    private final PasswordEncoder passwordEncoder;
    private final ConfigService configService;
    private final AppProperties properties;
    private final SensitivePolicyService sensitivePolicyService;
    private final OnlineSessionRegistry sessionRegistry;
    private final CacheService cache;
    private final ObjectProvider<UserProjectProvider> projectProvider;

    // ---------------- 查询 ----------------

    public PageResult<UserVO> page(UserDtos.Query query) {
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like(SysUser::getUsername, kw)
                    .or().like(SysUser::getRealName, kw)
                    .or().like(SysUser::getMobile, kw)
                    .or().like(SysUser::getEmployeeNo, kw));
        }
        if (query.getOrgId() != null) {
            List<Long> orgIds = Boolean.FALSE.equals(query.getIncludeChild())
                    ? List.of(query.getOrgId())
                    : dataScopeService.orgWithChildren(List.of(query.getOrgId()));
            wrapper.inSql(SysUser::getId, "select ur.user_id from sys_user_role ur"
                    + " join sys_role r on r.id = ur.role_id and r.is_deleted = 0"
                    + " where ur.is_deleted = 0 and r.org_id in (" + csv(orgIds) + ")");
        }
        if (query.getRoleIds() != null && !query.getRoleIds().isEmpty()) {
            String ids = csv(query.getRoleIds());
            if ("ALL".equalsIgnoreCase(query.getRoleMatch())) {
                wrapper.inSql(SysUser::getId, "select user_id from sys_user_role where is_deleted = 0"
                        + " and role_id in (" + ids + ") group by user_id having count(distinct role_id) = "
                        + query.getRoleIds().size());
            } else {
                wrapper.inSql(SysUser::getId, "select user_id from sys_user_role where is_deleted = 0"
                        + " and role_id in (" + ids + ")");
            }
        }
        if (query.getCompanyId() != null) {
            wrapper.inSql(SysUser::getId, "select user_id from sys_user_company where is_deleted = 0"
                    + " and company_id = " + query.getCompanyId());
        }
        if (query.getStatuses() != null && !query.getStatuses().isEmpty()) {
            wrapper.in(SysUser::getStatus, query.getStatuses());
        }
        if (StringUtils.hasText(query.getEmployeeType())) {
            wrapper.eq(SysUser::getEmployeeType, query.getEmployeeType());
        }
        if (query.getJoinStart() != null) {
            wrapper.ge(SysUser::getJoinDate, query.getJoinStart());
        }
        if (query.getJoinEnd() != null) {
            wrapper.le(SysUser::getJoinDate, query.getJoinEnd());
        }
        wrapper.orderByDesc(SysUser::getCreatedTime);

        Page<SysUser> page = userMapper.selectPage(
                new Page<>(query.safePageNum(), query.safePageSize()), wrapper);
        List<UserVO> list = toVOList(page.getRecords());
        return PageResult.of(list, page.getTotal(), query.safePageNum(), query.safePageSize());
    }

    public UserVO detail(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        List<UserVO> list = toVOList(List.of(user));
        UserVO vo = list.get(0);
        vo.setCompanies(userCompanyMapper.selectCompanyBriefs(List.of(id)));
        return vo;
    }

    /**
     * 项目参与 Tab：委托项目管理模块返回（R-09 双向可查），未装配时为空。
     */
    public List<Object> projects(Long userId) {
        UserProjectProvider provider = projectProvider.getIfAvailable();
        if (provider == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(provider.projectsOf(userId));
    }

    // ---------------- 写入 ----------------

    @Transactional
    public Long create(UserDtos.SaveRequest request) {
        validateUnique(request, null);
        validateRoles(request.getRoleIds());

        SysUser user = new SysUser();
        copy(request, user);
        user.setUsername(request.getUsername().trim());
        user.setPassword(passwordEncoder.encode(
                configService.get("user.init.password", properties.getSecurity().getInitialPassword())));
        user.setIdCard(CryptoUtil.encrypt(properties.getJwt().getSecret(), request.getIdCard()));
        user.setFirstLogin(1);
        user.setDefaultRoleId(request.getDefaultRoleId());
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        userMapper.insert(user);

        assignRoles(user.getId(), request.getRoleIds(), request.getDefaultRoleId());
        saveCompanies(user.getId(), request.getCompanies());
        return user.getId();
    }

    @Transactional
    public void update(UserDtos.SaveRequest request) {
        if (request.getId() == null) {
            throw BizException.badRequest("缺少用户 ID");
        }
        SysUser exists = userMapper.selectById(request.getId());
        if (exists == null) {
            throw BizException.notFound("用户不存在");
        }
        validateUnique(request, request.getId());
        validateRoles(request.getRoleIds());

        SysUser user = new SysUser();
        user.setId(request.getId());
        copy(request, user);
        user.setIdCard(CryptoUtil.encrypt(properties.getJwt().getSecret(), request.getIdCard()));
        user.setDefaultRoleId(request.getDefaultRoleId());
        if (Integer.valueOf(3).equals(request.getStatus())) {
            user.setLeaveDate(request.getLeaveDate() == null ? LocalDate.now() : request.getLeaveDate());
        }
        userMapper.updateById(user);

        assignRoles(request.getId(), request.getRoleIds(), request.getDefaultRoleId());
        saveCompanies(request.getId(), request.getCompanies());
    }

    @Transactional
    public void delete(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        if ("admin".equals(user.getUsername())) {
            throw BizException.biz("内置超管账号不允许删除");
        }
        userMapper.deleteById(id);
        userRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, id));
        userCompanyMapper.delete(Wrappers.<SysUserCompany>lambdaQuery().eq(SysUserCompany::getUserId, id));
        evictUserContext(id);
    }

    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds, Long defaultRoleId) {
        validateRoles(roleIds);
        List<SysRole> roles = roleMapper.selectBatchIds(roleIds);
        if (roles.size() != roleIds.size()) {
            throw BizException.badRequest("存在无效的角色");
        }
        Set<Long> orgIds = new HashSet<>();
        for (SysRole role : roles) {
            if (!orgIds.add(role.getOrgId())) {
                String orgName = roleContextService.orgName(role.getOrgId());
                throw BizException.biz("该用户在该组织（" + orgName + "）下已有角色，一个组织下同一用户只允许一个角色");
            }
        }
        if (!roleIds.contains(defaultRoleId)) {
            throw BizException.badRequest("默认角色必须是已分配的角色之一");
        }

        userRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, userId));
        for (Long roleId : roleIds) {
            SysUserRole relation = new SysUserRole();
            relation.setUserId(userId);
            relation.setRoleId(roleId);
            relation.setIsDefault(roleId.equals(defaultRoleId) ? 1 : 0);
            userRoleMapper.insert(relation);
        }
        SysUser update = new SysUser();
        update.setId(userId);
        update.setDefaultRoleId(defaultRoleId);
        userMapper.updateById(update);
        evictUserContext(userId);
    }

    @Transactional
    public void updateStatus(Long userId, UserDtos.StatusRequest request) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        if ("admin".equals(user.getUsername()) && !Integer.valueOf(1).equals(request.getStatus())) {
            throw BizException.biz("内置超管账号不允许停用或离职");
        }
        SysUser update = new SysUser();
        update.setId(userId);
        update.setStatus(request.getStatus());
        if (Integer.valueOf(3).equals(request.getStatus())) {
            update.setLeaveDate(request.getLeaveDate() == null ? LocalDate.now() : request.getLeaveDate());
        }
        userMapper.updateById(update);
        if (Integer.valueOf(2).equals(request.getStatus()) || Integer.valueOf(3).equals(request.getStatus())) {
            for (String sessionId : sessionRegistry.removeByUser(userId)) {
                cache.put("revoked:" + sessionId, Boolean.TRUE, properties.getJwt().getAccessTokenMinutes() * 60L);
            }
        }
        evictUserContext(userId);
    }

    public String resetPassword(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        String plain = configService.get("user.init.password", properties.getSecurity().getInitialPassword());
        SysUser update = new SysUser();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(plain));
        update.setFirstLogin(1);
        userMapper.updateById(update);
        return plain;
    }

    /**
     * 导出走同一套脱敏逻辑（列级屏障不允许被导出绕过）。
     */
    public byte[] exportCsv(UserDtos.Query query) {
        query.setPageNum(1);
        query.setPageSize(100);
        PageResult<UserVO> page = page(query);
        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');
        sb.append("姓名,登录账号,工号,组织,角色,手机号,邮箱,状态,入职日期\n");
        for (UserVO vo : page.getList()) {
            sb.append(nvl(vo.getRealName())).append(',')
                    .append(nvl(vo.getUsername())).append(',')
                    .append(nvl(vo.getEmployeeNo())).append(',')
                    .append(nvl(vo.getOrgName())).append(',')
                    .append(nvl(vo.getRoles() == null ? "" : vo.getRoles().stream()
                            .map(r -> r.getOrgName() + "/" + r.getRoleName())
                            .collect(Collectors.joining(" ")))).append(',')
                    .append(sensitivePolicyService.mask("MOBILE", "mobile", vo.getMobile())).append(',')
                    .append(sensitivePolicyService.mask("EMAIL", "email", vo.getEmail())).append(',')
                    .append(statusLabel(vo.getStatus())).append(',')
                    .append(vo.getJoinDate() == null ? "" : vo.getJoinDate())
                    .append('\n');
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    // ---------------- 内部 ----------------

    private List<UserVO> toVOList(List<SysUser> users) {
        if (users.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> ids = users.stream().map(SysUser::getId).toList();
        List<UserRoleBrief> briefs = userRoleMapper.selectRoleBriefs(ids);
        Map<Long, List<UserRoleBrief>> roleMap = new HashMap<>();
        for (UserRoleBrief brief : briefs) {
            roleMap.computeIfAbsent(brief.getUserId(), k -> new ArrayList<>()).add(brief);
        }
        Map<Long, List<UserCompanyBrief>> companyMap = new HashMap<>();
        for (UserCompanyBrief brief : userCompanyMapper.selectCompanyBriefs(ids)) {
            companyMap.computeIfAbsent(brief.getUserId(), k -> new ArrayList<>()).add(brief);
        }
        Map<Long, String> orgPathCache = new LinkedHashMap<>();
        List<UserVO> result = new ArrayList<>();
        for (SysUser user : users) {
            UserVO vo = new UserVO();
            vo.setId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setRealName(user.getRealName());
            vo.setAvatar(user.getAvatar());
            vo.setEmployeeNo(user.getEmployeeNo());
            vo.setMobile(user.getMobile());
            vo.setEmail(user.getEmail());
            vo.setIdCard(CryptoUtil.decrypt(properties.getJwt().getSecret(), user.getIdCard()));
            vo.setGender(user.getGender());
            vo.setEmployeeType(user.getEmployeeType());
            vo.setEmploymentForm(user.getEmploymentForm());
            vo.setPosition(user.getPosition());
            vo.setJobLevel(user.getJobLevel());
            vo.setJoinDate(user.getJoinDate());
            vo.setLeaveDate(user.getLeaveDate());
            vo.setCertStatus(user.getCertStatus());
            vo.setCertItem(user.getCertItem());
            vo.setCertExpireDate(user.getCertExpireDate());
            vo.setStatus(user.getStatus());
            vo.setDefaultRoleId(user.getDefaultRoleId());
            vo.setLastLoginTime(user.getLastLoginTime());
            vo.setRemark(user.getRemark());
            List<UserRoleBrief> roles = roleMap.getOrDefault(user.getId(), new ArrayList<>());
            vo.setRoles(roles);
            UserRoleBrief defaultRole = roles.stream()
                    .filter(r -> r.getRoleId().equals(user.getDefaultRoleId()))
                    .findFirst()
                    .orElse(roles.isEmpty() ? null : roles.get(0));
            if (defaultRole != null) {
                vo.setOrgId(defaultRole.getOrgId());
                vo.setOrgName(defaultRole.getOrgName());
                vo.setOrgPath(orgPathCache.computeIfAbsent(defaultRole.getOrgId(),
                        roleContextService::buildOrgPath));
            }
            List<UserCompanyBrief> companies = companyMap.getOrDefault(user.getId(), new ArrayList<>());
            vo.setCompanies(companies);
            vo.setCompanyNames(companies.stream()
                    .filter(c -> Integer.valueOf(1).equals(c.getRelationType()))
                    .map(UserCompanyBrief::getCompanyName)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.joining("、")));
            result.add(vo);
        }
        return result;
    }

    private void validateUnique(UserDtos.SaveRequest request, Long excludeId) {
        String username = request.getUsername().trim();
        Long count = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, username)
                .ne(excludeId != null, SysUser::getId, excludeId));
        if (count != null && count > 0) {
            throw BizException.biz("登录账号「" + username + "」已存在");
        }
        Long mobileCount = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getMobile, request.getMobile())
                .ne(excludeId != null, SysUser::getId, excludeId));
        if (mobileCount != null && mobileCount > 0) {
            throw BizException.biz("手机号「" + request.getMobile() + "」已被占用");
        }
        if (StringUtils.hasText(request.getEmployeeNo())) {
            Long noCount = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                    .eq(SysUser::getEmployeeNo, request.getEmployeeNo())
                    .ne(excludeId != null, SysUser::getId, excludeId));
            if (noCount != null && noCount > 0) {
                throw BizException.biz("工号「" + request.getEmployeeNo() + "」已被占用");
            }
        }
        if (Integer.valueOf(3).equals(request.getStatus())
                && request.getJoinDate() != null && request.getLeaveDate() != null
                && request.getLeaveDate().isBefore(request.getJoinDate())) {
            throw BizException.badRequest("离职日期不得早于入职日期");
        }
    }

    private void validateRoles(List<Long> roleIds) {
        int max = configService.getInt("role.max.per.user", 5);
        if (roleIds == null || roleIds.isEmpty()) {
            throw BizException.biz("请至少分配一个角色");
        }
        if (roleIds.size() > max) {
            throw BizException.biz("单个用户最多关联 " + max + " 个角色");
        }
        if (new HashSet<>(roleIds).size() != roleIds.size()) {
            throw BizException.badRequest("角色不可重复");
        }
    }

    private void saveCompanies(Long userId, List<UserDtos.CompanyRelation> relations) {
        userCompanyMapper.delete(Wrappers.<SysUserCompany>lambdaQuery().eq(SysUserCompany::getUserId, userId));
        if (relations == null || relations.isEmpty()) {
            return;
        }
        Set<Long> validCompanyIds = companyMapper.selectBatchIds(
                        relations.stream().map(UserDtos.CompanyRelation::getCompanyId).distinct().toList())
                .stream().map(SysCompany::getId).collect(Collectors.toSet());
        for (UserDtos.CompanyRelation relation : relations) {
            if (!validCompanyIds.contains(relation.getCompanyId())) {
                throw BizException.badRequest("公司主体不存在：" + relation.getCompanyId());
            }
            SysUserCompany entity = new SysUserCompany();
            entity.setUserId(userId);
            entity.setCompanyId(relation.getCompanyId());
            entity.setRelationType(relation.getRelationType());
            entity.setIsPrimary(relation.getIsPrimary() == null ? 0 : relation.getIsPrimary());
            userCompanyMapper.insert(entity);
        }
        Long contractCount = relations.stream()
                .filter(r -> Integer.valueOf(2).equals(r.getRelationType())).count();
        if (contractCount > 1) {
            throw BizException.biz("劳动合同主体只能有一个");
        }
    }

    private void copy(UserDtos.SaveRequest request, SysUser user) {
        user.setRealName(request.getRealName());
        user.setEmployeeNo(request.getEmployeeNo());
        user.setMobile(request.getMobile());
        user.setEmail(request.getEmail());
        user.setGender(request.getGender());
        user.setEmployeeType(request.getEmployeeType());
        user.setEmploymentForm(request.getEmploymentForm());
        user.setPosition(request.getPosition());
        user.setJobLevel(request.getJobLevel());
        user.setJoinDate(request.getJoinDate());
        user.setCertStatus(request.getCertStatus());
        user.setCertItem(request.getCertItem());
        user.setCertExpireDate(request.getCertExpireDate());
        user.setStatus(request.getStatus());
        user.setRemark(request.getRemark());
    }

    private void evictUserContext(Long userId) {
        cache.evictByPrefix("rolectx:" + userId + ":");
    }

    private String csv(List<Long> ids) {
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private String nvl(String value) {
        return value == null ? "" : value.replace(",", "，");
    }

    private String statusLabel(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case 1 -> "正常";
            case 2 -> "停用";
            case 3 -> "离职";
            case 4 -> "锁定";
            default -> "未知";
        };
    }
}
