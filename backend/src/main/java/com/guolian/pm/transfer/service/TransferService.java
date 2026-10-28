package com.guolian.pm.transfer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guolian.pm.auth.RoleContextService;
import com.guolian.pm.common.api.PageQuery;
import com.guolian.pm.common.api.PageResult;
import com.guolian.pm.common.config.AppProperties;
import com.guolian.pm.common.exception.BizException;
import com.guolian.pm.common.web.LoginUser;
import com.guolian.pm.flow.dto.FlowDtos;
import com.guolian.pm.flow.event.FlowFinishedEvent;
import com.guolian.pm.flow.service.FlowService;
import com.guolian.pm.system.config.service.ConfigService;
import com.guolian.pm.system.role.entity.SysRole;
import com.guolian.pm.system.role.mapper.SysRoleMapper;
import com.guolian.pm.system.user.entity.SysUser;
import com.guolian.pm.system.user.mapper.SysUserMapper;
import com.guolian.pm.system.user.mapper.SysUserRoleMapper;
import com.guolian.pm.transfer.adapter.TransferAdapter;
import com.guolian.pm.transfer.dto.TransferDtos;
import com.guolian.pm.transfer.entity.SysAccountTransfer;
import com.guolian.pm.transfer.entity.SysAccountTransferItem;
import com.guolian.pm.transfer.mapper.SysAccountTransferItemMapper;
import com.guolian.pm.transfer.mapper.SysAccountTransferMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 账号交接：适配器预检 → 接收人校验 → 提交审批 → 审批通过后异步执行 → 可撤销期内回写。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SysAccountTransferMapper transferMapper;
    private final SysAccountTransferItemMapper itemMapper;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final RoleContextService roleContextService;
    private final FlowService flowService;
    private final ConfigService configService;
    private final AppProperties properties;
    private final ObjectMapper objectMapper;
    private final List<TransferAdapter> adapters;
    /** 用于让 execute() 走 @Async 代理，避免自调用失效 */
    private final org.springframework.beans.factory.ObjectProvider<TransferService> self;

    // ---------------- 查询 ----------------

    public PageResult<TransferDtos.DetailVO> page(PageQuery query, String flowStatus, String execStatus, String keyword) {
        LambdaQueryWrapper<SysAccountTransfer> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(flowStatus)) {
            wrapper.eq(SysAccountTransfer::getFlowStatus, flowStatus);
        }
        if (StringUtils.hasText(execStatus)) {
            wrapper.eq(SysAccountTransfer::getExecStatus, execStatus);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(SysAccountTransfer::getTransferNo, keyword);
        }
        wrapper.orderByDesc(SysAccountTransfer::getCreatedTime);
        Page<SysAccountTransfer> page = transferMapper.selectPage(
                new Page<>(query.safePageNum(), query.safePageSize()), wrapper);
        List<TransferDtos.DetailVO> list = page.getRecords().stream().map(this::toDetail).toList();
        return PageResult.of(list, page.getTotal(), query.safePageNum(), query.safePageSize());
    }

    public TransferDtos.DetailVO detail(Long id) {
        return toDetail(require(id));
    }

    public List<TransferDtos.ItemVO> items(Long id) {
        List<SysAccountTransferItem> list = itemMapper.selectList(Wrappers.<SysAccountTransferItem>lambdaQuery()
                .eq(SysAccountTransferItem::getTransferId, id)
                .orderByAsc(SysAccountTransferItem::getId));
        List<TransferDtos.ItemVO> result = new ArrayList<>();
        for (SysAccountTransferItem item : list) {
            TransferDtos.ItemVO vo = new TransferDtos.ItemVO();
            vo.setId(item.getId());
            vo.setBizType(item.getBizType());
            vo.setBizId(item.getBizId());
            vo.setBizName(item.getBizName());
            vo.setBeforeOwnerId(item.getBeforeOwnerId());
            vo.setAfterOwnerId(item.getAfterOwnerId());
            vo.setBeforeOwnerName(userName(item.getBeforeOwnerId()));
            vo.setAfterOwnerName(userName(item.getAfterOwnerId()));
            vo.setResult(item.getResult());
            vo.setFailReason(item.getFailReason());
            result.add(vo);
        }
        return result;
    }

    public List<TransferDtos.AdapterInfo> adapters() {
        List<TransferDtos.AdapterInfo> result = new ArrayList<>();
        for (TransferAdapter adapter : adapters) {
            TransferDtos.AdapterInfo info = new TransferDtos.AdapterInfo();
            info.setBizType(adapter.bizType());
            info.setBizName(adapter.bizName());
            info.setAvailable(true);
            result.add(info);
        }
        return result;
    }

    /**
     * 预检：遍历已注册适配器统计待交接数量，为 0 的对象置灰。
     */
    public TransferDtos.PrecheckVO precheck(Long fromUserId) {
        SysUser from = userMapper.selectById(fromUserId);
        if (from == null) {
            throw BizException.notFound("交出人不存在");
        }
        TransferDtos.PrecheckVO vo = new TransferDtos.PrecheckVO();
        vo.setFromUserId(fromUserId);
        vo.setFromUserName(from.getRealName());
        List<Long> roleIds = userRoleMapper.selectRoleIds(fromUserId);
        long total = 0;
        for (Long roleId : roleIds) {
            SysRole role = roleMapper.selectById(roleId);
            if (role == null) {
                continue;
            }
            TransferDtos.RoleBrief brief = new TransferDtos.RoleBrief();
            brief.setRoleId(role.getId());
            brief.setRoleCode(role.getRoleCode());
            brief.setRoleName(role.getRoleName());
            brief.setOrgId(role.getOrgId());
            brief.setOrgName(roleContextService.orgName(role.getOrgId()));
            brief.setDefault(roleId.equals(from.getDefaultRoleId()));
            vo.getRoles().add(brief);
        }
        for (TransferAdapter adapter : adapters) {
            TransferDtos.AdapterInfo info = new TransferDtos.AdapterInfo();
            info.setBizType(adapter.bizType());
            info.setBizName(adapter.bizName());
            long count = adapter.count(fromUserId, null);
            info.setCount(count);
            info.setAvailable(count > 0);
            total += count;
            vo.getAdapters().add(info);
        }
        vo.setTotalCount(total);
        return vo;
    }

    /**
     * 接收人资格：必须拥有对应角色、状态正常、非交出人本人。
     */
    public List<TransferDtos.UserBrief> eligibleReceivers(Long fromUserId, Long roleId) {
        List<Long> userIds = userRoleMapper.selectList(Wrappers.<com.guolian.pm.system.user.entity.SysUserRole>lambdaQuery()
                        .eq(com.guolian.pm.system.user.entity.SysUserRole::getRoleId, roleId))
                .stream().map(com.guolian.pm.system.user.entity.SysUserRole::getUserId)
                .filter(id -> !id.equals(fromUserId))
                .toList();
        if (userIds.isEmpty()) {
            return List.of();
        }
        List<SysUser> users = userMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                .in(SysUser::getId, userIds)
                .eq(SysUser::getStatus, 1));
        List<TransferDtos.UserBrief> result = new ArrayList<>();
        for (SysUser user : users) {
            TransferDtos.UserBrief brief = new TransferDtos.UserBrief();
            brief.setUserId(user.getId());
            brief.setUsername(user.getUsername());
            brief.setRealName(user.getRealName());
            SysRole role = roleMapper.selectById(roleId);
            brief.setOrgName(role == null ? null : roleContextService.orgName(role.getOrgId()));
            result.add(brief);
        }
        return result;
    }

    // ---------------- 写入 ----------------

    @Transactional
    public Long create(TransferDtos.CreateRequest request, LoginUser operator) {
        SysUser from = userMapper.selectById(request.getFromUserId());
        if (from == null) {
            throw BizException.notFound("交出人不存在");
        }
        Long blocked = transferMapper.selectCount(Wrappers.<SysAccountTransfer>lambdaQuery()
                .eq(SysAccountTransfer::getFromUserId, request.getFromUserId())
                .and(w -> w.eq(SysAccountTransfer::getFlowStatus, "RUNNING")
                        .or().eq(SysAccountTransfer::getExecStatus, "EXECUTING")));
        if (blocked != null && blocked > 0) {
            throw BizException.biz("该交出人已有审批中或执行中的交接单，请先完成");
        }
        Set<String> registered = adapters.stream().map(TransferAdapter::bizType).collect(Collectors.toSet());
        for (String bizType : request.getScope()) {
            if (!registered.contains(bizType)) {
                throw BizException.biz("业务对象「" + bizType + "」暂未接入交接");
            }
        }
        validateReceivers(request);

        SysAccountTransfer entity = new SysAccountTransfer();
        entity.setTransferNo(nextTransferNo());
        entity.setFromUserId(request.getFromUserId());
        entity.setToUserIds(toJson(request.getToUserIds()));
        entity.setAssignMode(request.getAssignMode() == null ? 1 : request.getAssignMode());
        entity.setScope(toJson(request.getScope()));
        entity.setRoleAssignments(toRoleAssignmentJson(request.getRoleAssignments()));
        entity.setRemark(request.getRemark());
        entity.setEffectiveTime(request.getEffectiveTime() == null ? LocalDateTime.now() : request.getEffectiveTime());
        entity.setFlowStatus("DRAFT");
        entity.setExecStatus("NOT_EXECUTED");
        entity.setItemCount(0);
        entity.setFailCount(0);
        transferMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 提交审批：启动 Flowable 流程，单据锁定为审批中，审批通过前不得执行任何数据迁移。
     */
    @Transactional
    public String submit(Long id, LoginUser operator) {
        SysAccountTransfer entity = require(id);
        if (!List.of("DRAFT", "REJECTED").contains(entity.getFlowStatus())) {
            throw BizException.biz("当前状态（" + entity.getFlowStatus() + "）不可提交审批");
        }
        SysUser from = userMapper.selectById(entity.getFromUserId());
        String toNames = parseIds(entity.getToUserIds()).stream()
                .map(this::userName).collect(Collectors.joining("、"));

        FlowDtos.StartRequest request = new FlowDtos.StartRequest();
        request.setBizType("ACCOUNT_TRANSFER");
        request.setBizId(id);
        request.setBizNo(entity.getTransferNo());
        request.setBizTitle("账号交接：" + (from == null ? "" : from.getRealName()) + " → " + toNames);
        request.setDetailRoute("/system/transfer?id=" + id);
        // 直属上级 / 组织负责人按交出人取，不按操作人取
        request.getVariables().put("subjectUserId", entity.getFromUserId());
        String instanceId = flowService.start(request, operator);

        SysAccountTransfer update = new SysAccountTransfer();
        update.setId(id);
        update.setFlowStatus("RUNNING");
        update.setFlowInstanceId(instanceId);
        update.setExecStatus("NOT_EXECUTED");
        transferMapper.updateById(update);
        return instanceId;
    }

    /**
     * 审批结束回调：通过则触发异步执行；驳回/撤回保持未执行，允许修改后重新提交。
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFlowFinished(FlowFinishedEvent event) {
        if (!"ACCOUNT_TRANSFER".equals(event.bizType())) {
            return;
        }
        SysAccountTransfer entity = transferMapper.selectById(event.bizId());
        if (entity == null) {
            return;
        }
        SysAccountTransfer update = new SysAccountTransfer();
        update.setId(entity.getId());
        if (event.approved()) {
            int revokeDays = configService.getInt("transfer.revoke.days", properties.getTransfer().getRevokeDays());
            update.setFlowStatus("APPROVED");
            update.setRevokeDeadline(LocalDateTime.now().plusDays(revokeDays));
            transferMapper.updateById(update);
            executeAsync(entity.getId());
        } else {
            update.setFlowStatus("REJECTED");
            update.setExecStatus("NOT_EXECUTED");
            transferMapper.updateById(update);
        }
    }

    public void execute(Long id) {
        SysAccountTransfer entity = require(id);
        if (!"APPROVED".equals(entity.getFlowStatus())) {
            throw BizException.biz("审批通过后才允许执行交接");
        }
        if (!List.of("NOT_EXECUTED", "PARTIAL_FAILED").contains(entity.getExecStatus())) {
            throw BizException.biz("当前执行状态不可重复执行");
        }
        self.getObject().executeAsync(id);
    }

    /**
     * 异步执行：按适配器逐类迁移，超过阈值分批，逐条写明细。不阻塞请求线程。
     */
    @Async
    public void executeAsync(Long id) {
        SysAccountTransfer entity = transferMapper.selectById(id);
        if (entity == null || !"APPROVED".equals(entity.getFlowStatus())) {
            return;
        }
        SysAccountTransfer running = new SysAccountTransfer();
        running.setId(id);
        running.setExecStatus("EXECUTING");
        transferMapper.updateById(running);

        List<String> scope = parseStrings(entity.getScope());
        Map<String, Long> roleAssignments = parseRoleAssignments(entity.getRoleAssignments());
        Long defaultReceiver = parseIds(entity.getToUserIds()).stream().findFirst().orElse(null);
        int batch = properties.getTransfer().getBatchSize();
        int maxItems = configService.getInt("transfer.max.items.per.type", properties.getTransfer().getMaxItemsPerType());
        int success = 0;
        int failed = 0;

        for (String bizType : scope) {
            TransferAdapter adapter = findAdapter(bizType);
            if (adapter == null) {
                continue;
            }
            long total = adapter.count(entity.getFromUserId(), null);
            int processed = 0;
            while (processed < total && processed < maxItems) {
                List<Long> ids = adapter.listIds(entity.getFromUserId(), processed, batch);
                if (ids.isEmpty()) {
                    break;
                }
                Map<Long, List<Long>> byReceiver = new HashMap<>();
                for (Long bizId : ids) {
                    Long receiver = defaultReceiver;
                    String ownerRole = adapter.ownerRoleCode(bizId);
                    if ("2".equals(String.valueOf(entity.getAssignMode())) && ownerRole != null) {
                        Long assigned = roleAssignments.get(ownerRole);
                        if (assigned != null) {
                            receiver = assigned;
                        }
                    }
                    if (receiver == null) {
                        continue;
                    }
                    byReceiver.computeIfAbsent(receiver, k -> new ArrayList<>()).add(bizId);
                }
                for (Map.Entry<Long, List<Long>> entry : byReceiver.entrySet()) {
                    TransferAdapter.TransferOutcome outcome = adapter.transfer(
                            entity.getFromUserId(), entry.getKey(), entry.getValue());
                    for (Long bizId : outcome.succeeded()) {
                        saveItem(entity, adapter, bizId, entry.getKey(), "SUCCESS", null);
                        success++;
                    }
                    for (Map.Entry<Long, String> fail : outcome.failed().entrySet()) {
                        saveItem(entity, adapter, fail.getKey(), entry.getKey(), "FAILED", fail.getValue());
                        failed++;
                    }
                }
                processed += ids.size();
            }
        }

        SysAccountTransfer done = new SysAccountTransfer();
        done.setId(id);
        done.setExecStatus(failed == 0 ? "DONE" : "PARTIAL_FAILED");
        done.setItemCount(success + failed);
        done.setFailCount(failed);
        transferMapper.updateById(done);
        log.info("账号交接执行完成 transferId={} success={} failed={}", id, success, failed);
    }

    @Transactional
    public void retry(Long id) {
        SysAccountTransfer entity = require(id);
        if (!"PARTIAL_FAILED".equals(entity.getExecStatus())) {
            throw BizException.biz("仅部分失败状态允许重试失败项");
        }
        List<SysAccountTransferItem> failedItems = itemMapper.selectList(Wrappers.<SysAccountTransferItem>lambdaQuery()
                .eq(SysAccountTransferItem::getTransferId, id)
                .eq(SysAccountTransferItem::getResult, "FAILED"));
        Map<String, List<SysAccountTransferItem>> grouped = failedItems.stream()
                .collect(Collectors.groupingBy(SysAccountTransferItem::getBizType));
        int remainFail = 0;
        for (Map.Entry<String, List<SysAccountTransferItem>> entry : grouped.entrySet()) {
            TransferAdapter adapter = findAdapter(entry.getKey());
            if (adapter == null) {
                remainFail += entry.getValue().size();
                continue;
            }
            Map<Long, List<SysAccountTransferItem>> byReceiver = entry.getValue().stream()
                    .collect(Collectors.groupingBy(SysAccountTransferItem::getAfterOwnerId));
            for (Map.Entry<Long, List<SysAccountTransferItem>> receiverGroup : byReceiver.entrySet()) {
                List<Long> ids = receiverGroup.getValue().stream()
                        .map(SysAccountTransferItem::getBizId).toList();
                TransferAdapter.TransferOutcome outcome = adapter.transfer(
                        entity.getFromUserId(), receiverGroup.getKey(), ids);
                for (Long bizId : outcome.succeeded()) {
                    updateItemResult(id, bizId, "SUCCESS", null);
                }
                remainFail += outcome.failed().size();
                outcome.failed().forEach((bizId, reason) ->
                        updateItemResult(id, bizId, "FAILED", reason));
            }
        }
        SysAccountTransfer update = new SysAccountTransfer();
        update.setId(id);
        update.setExecStatus(remainFail == 0 ? "DONE" : "PARTIAL_FAILED");
        update.setFailCount(remainFail);
        transferMapper.updateById(update);
    }

    /**
     * 撤销：可撤销期内按明细反向回写。
     */
    @Transactional
    public void revoke(Long id, String reason) {
        SysAccountTransfer entity = require(id);
        if (!List.of("DONE", "PARTIAL_FAILED").contains(entity.getExecStatus())) {
            throw BizException.biz("当前状态不可撤销");
        }
        if (entity.getRevokeDeadline() != null && LocalDateTime.now().isAfter(entity.getRevokeDeadline())) {
            throw BizException.biz("已超过可撤销期，无法撤销");
        }
        List<SysAccountTransferItem> successItems = itemMapper.selectList(
                Wrappers.<SysAccountTransferItem>lambdaQuery()
                        .eq(SysAccountTransferItem::getTransferId, id)
                        .eq(SysAccountTransferItem::getResult, "SUCCESS"));
        Map<String, List<SysAccountTransferItem>> grouped = successItems.stream()
                .collect(Collectors.groupingBy(SysAccountTransferItem::getBizType));
        for (Map.Entry<String, List<SysAccountTransferItem>> entry : grouped.entrySet()) {
            TransferAdapter adapter = findAdapter(entry.getKey());
            if (adapter == null) {
                continue;
            }
            List<TransferAdapter.RollbackItem> rollbackItems = entry.getValue().stream()
                    .map(i -> new TransferAdapter.RollbackItem(i.getBizId(), i.getBeforeOwnerId(), i.getAfterOwnerId()))
                    .toList();
            adapter.rollback(rollbackItems);
            for (SysAccountTransferItem item : entry.getValue()) {
                updateItemResult(id, item.getBizId(), "ROLLED_BACK", reason);
            }
        }
        SysAccountTransfer update = new SysAccountTransfer();
        update.setId(id);
        update.setExecStatus("REVOKED");
        transferMapper.updateById(update);
    }

    // ---------------- 内部 ----------------

    private void validateReceivers(TransferDtos.CreateRequest request) {
        List<Long> fromRoleIds = userRoleMapper.selectRoleIds(request.getFromUserId());
        Set<Long> fromRoleSet = new HashSet<>(fromRoleIds);
        int mode = request.getAssignMode() == null ? 1 : request.getAssignMode();
        if (mode == 1) {
            for (Long toUserId : request.getToUserIds()) {
                validateReceiverBase(toUserId, request.getFromUserId());
                Set<Long> toRoles = new HashSet<>(userRoleMapper.selectRoleIds(toUserId));
                List<String> missing = fromRoleSet.stream()
                        .filter(id -> !toRoles.contains(id))
                        .map(id -> {
                            SysRole role = roleMapper.selectById(id);
                            return role == null ? String.valueOf(id) : role.getRoleName();
                        }).toList();
                if (!missing.isEmpty()) {
                    throw BizException.biz("接收人缺少角色：" + String.join("、", missing)
                            + "；单接收人模式要求接收人拥有交出人的全部角色，可切换为按角色分派");
                }
            }
        } else {
            for (Long roleId : fromRoleIds) {
                Long receiver = request.getRoleAssignments().get(roleId);
                SysRole role = roleMapper.selectById(roleId);
                if (receiver == null) {
                    throw BizException.biz("请为角色「" + (role == null ? roleId : role.getRoleName()) + "」指定接收人");
                }
                validateReceiverBase(receiver, request.getFromUserId());
                if (!userRoleMapper.selectRoleIds(receiver).contains(roleId)) {
                    throw BizException.biz("接收人不拥有角色「" + (role == null ? roleId : role.getRoleName()) + "」");
                }
            }
        }
    }

    private void validateReceiverBase(Long userId, Long fromUserId) {
        if (userId.equals(fromUserId)) {
            throw BizException.biz("接收人不能是交出人本人");
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw BizException.notFound("接收人不存在");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw BizException.biz("接收人「" + user.getRealName() + "」状态不正常，无法接收交接");
        }
    }

    private void saveItem(SysAccountTransfer entity, TransferAdapter adapter, Long bizId,
                          Long receiverId, String result, String reason) {
        SysAccountTransferItem item = new SysAccountTransferItem();
        item.setTransferId(entity.getId());
        item.setBizType(adapter.bizType());
        item.setBizId(bizId);
        item.setBizName(adapter.describe(bizId));
        item.setBeforeOwnerId(entity.getFromUserId());
        item.setAfterOwnerId(receiverId);
        item.setResult(result);
        item.setFailReason(reason);
        itemMapper.insert(item);
    }

    private void updateItemResult(Long transferId, Long bizId, String result, String reason) {
        SysAccountTransferItem update = new SysAccountTransferItem();
        update.setResult(result);
        update.setFailReason(reason);
        itemMapper.update(update, Wrappers.<SysAccountTransferItem>lambdaUpdate()
                .eq(SysAccountTransferItem::getTransferId, transferId)
                .eq(SysAccountTransferItem::getBizId, bizId));
    }

    private TransferAdapter findAdapter(String bizType) {
        return adapters.stream().filter(a -> a.bizType().equals(bizType)).findFirst().orElse(null);
    }

    private SysAccountTransfer require(Long id) {
        SysAccountTransfer entity = transferMapper.selectById(id);
        if (entity == null) {
            throw BizException.notFound("交接单不存在");
        }
        return entity;
    }

    private String nextTransferNo() {
        String day = LocalDate.now().format(DAY);
        String prefix = "HO-" + day + "-";
        Long count = transferMapper.selectCount(Wrappers.<SysAccountTransfer>lambdaQuery()
                .likeRight(SysAccountTransfer::getTransferNo, prefix));
        long seq = (count == null ? 0 : count) + 1;
        return prefix + String.format("%04d", seq);
    }

    private TransferDtos.DetailVO toDetail(SysAccountTransfer entity) {
        TransferDtos.DetailVO vo = new TransferDtos.DetailVO();
        vo.setId(entity.getId());
        vo.setTransferNo(entity.getTransferNo());
        vo.setFromUserId(entity.getFromUserId());
        vo.setFromUserName(userName(entity.getFromUserId()));
        List<Long> toIds = parseIds(entity.getToUserIds());
        vo.setToUserIds(toIds);
        vo.setToUserNames(toIds.stream().map(this::userName).collect(Collectors.joining("、")));
        vo.setAssignMode(entity.getAssignMode());
        vo.setScope(parseStrings(entity.getScope()));
        vo.setRemark(entity.getRemark());
        vo.setFlowStatus(entity.getFlowStatus());
        vo.setExecStatus(entity.getExecStatus());
        vo.setFlowInstanceId(entity.getFlowInstanceId());
        vo.setItemCount(entity.getItemCount());
        vo.setFailCount(entity.getFailCount());
        vo.setEffectiveTime(entity.getEffectiveTime());
        vo.setCreatedTime(entity.getCreatedTime() == null ? null : entity.getCreatedTime().format(FMT));
        boolean revocable = entity.getRevokeDeadline() != null
                && LocalDateTime.now().isBefore(entity.getRevokeDeadline())
                && List.of("DONE", "PARTIAL_FAILED").contains(entity.getExecStatus());
        vo.setRevocable(revocable);
        vo.setRevokeDeadline(entity.getRevokeDeadline() == null ? null : entity.getRevokeDeadline().format(FMT));
        return vo;
    }

    private String userName(Long userId) {
        if (userId == null) {
            return null;
        }
        SysUser user = userMapper.selectById(userId);
        return user == null ? String.valueOf(userId) : user.getRealName();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<Long> parseIds(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            List<Long> ids = objectMapper.readValue(json, new TypeReference<List<Long>>() {
            });
            return ids == null ? List.of() : ids;
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<String> parseStrings(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            List<String> list = objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
            return list == null ? List.of() : list;
        } catch (Exception e) {
            return List.of();
        }
    }

    /** roleId → userId 转成 roleCode → userId，供执行期按角色路由 */
    private String toRoleAssignmentJson(Map<Long, Long> assignments) {
        Map<String, Long> byCode = new LinkedHashMap<>();
        if (assignments != null) {
            for (Map.Entry<Long, Long> entry : assignments.entrySet()) {
                SysRole role = roleMapper.selectById(entry.getKey());
                if (role != null) {
                    byCode.put(role.getRoleCode(), entry.getValue());
                }
            }
        }
        return toJson(byCode);
    }

    private Map<String, Long> parseRoleAssignments(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Long>>() {
            });
        } catch (Exception e) {
            return Map.of();
        }
    }
}
