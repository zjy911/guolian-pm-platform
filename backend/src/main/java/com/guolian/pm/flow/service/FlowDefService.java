package com.guolian.pm.flow.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guolian.pm.common.exception.BizException;
import com.guolian.pm.flow.dto.FlowDtos;
import com.guolian.pm.flow.entity.SysFlowDef;
import com.guolian.pm.flow.mapper.SysFlowDefMapper;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 审批模板：表单化配置 → 生成 BPMN → 部署到 Flowable → 回写 key / version。
 */
@Service
@RequiredArgsConstructor
public class FlowDefService {

    private final SysFlowDefMapper flowDefMapper;
    private final BpmnGenerator bpmnGenerator;
    private final RepositoryService repositoryService;

    public List<SysFlowDef> list(String bizType, Integer status) {
        var wrapper = Wrappers.<SysFlowDef>lambdaQuery();
        if (StringUtils.hasText(bizType)) {
            wrapper.eq(SysFlowDef::getBizType, bizType);
        }
        if (status != null) {
            wrapper.eq(SysFlowDef::getStatus, status);
        }
        return flowDefMapper.selectList(wrapper.orderByAsc(SysFlowDef::getFlowCode));
    }

    public SysFlowDef get(Long id) {
        SysFlowDef def = flowDefMapper.selectById(id);
        if (def == null) {
            throw BizException.notFound("审批模板不存在");
        }
        return def;
    }

    @Transactional
    public Long save(FlowDtos.DefRequest request) {
        var nodes = bpmnGenerator.parseNodes(request.getNodeConfig());
        boolean hasApproval = nodes.stream().anyMatch(n -> !"START".equals(n.getType()) && !"END".equals(n.getType()));
        if (!hasApproval) {
            throw BizException.biz("流程至少需要一个审批节点");
        }
        Long exists = flowDefMapper.selectCount(Wrappers.<SysFlowDef>lambdaQuery()
                .eq(SysFlowDef::getFlowCode, request.getFlowCode())
                .ne(request.getId() != null, SysFlowDef::getId, request.getId()));
        if (exists != null && exists > 0) {
            throw BizException.biz("流程编码「" + request.getFlowCode() + "」已存在");
        }
        SysFlowDef def = new SysFlowDef();
        def.setId(request.getId());
        def.setFlowCode(request.getFlowCode());
        def.setFlowName(request.getFlowName());
        def.setBizType(request.getBizType());
        def.setOrgScope(request.getOrgScope());
        def.setFormCode(request.getFormCode());
        def.setNodeConfig(request.getNodeConfig());
        def.setProcessDefinitionKey(request.getProcessDefinitionKey());
        def.setStatus(request.getStatus() == null ? 0 : request.getStatus());
        if (def.getId() == null) {
            flowDefMapper.insert(def);
        } else {
            flowDefMapper.updateById(def);
        }
        return def.getId();
    }

    /**
     * 发布：生成 BPMN XML → 部署 → 回写 processDefinitionKey + deployedVersion。
     * 在途实例继续走其发起时的版本（Flowable 原生行为）。
     */
    @Transactional
    public SysFlowDef publish(Long id) {
        SysFlowDef def = get(id);
        List<FlowDtos.NodeConfig> nodes = bpmnGenerator.parseNodes(def.getNodeConfig());
        String key = StringUtils.hasText(def.getProcessDefinitionKey())
                ? def.getProcessDefinitionKey() : def.getFlowCode();
        String xml = bpmnGenerator.generate(key, def.getFlowName(), nodes);
        Deployment deployment = repositoryService.createDeployment()
                .name(def.getFlowName())
                .addString(key + ".bpmn20.xml", xml)
                .deploy();
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId())
                .singleResult();
        if (definition == null) {
            throw BizException.biz("流程部署失败，未生成流程定义");
        }
        SysFlowDef update = new SysFlowDef();
        update.setId(id);
        update.setProcessDefinitionKey(definition.getKey());
        update.setDeployedVersion(definition.getVersion());
        update.setStatus(1);
        flowDefMapper.updateById(update);
        return get(id);
    }

    @Transactional
    public void delete(Long id) {
        flowDefMapper.deleteById(id);
    }

    public SysFlowDef findByBizType(String bizType) {
        return flowDefMapper.selectOne(Wrappers.<SysFlowDef>lambdaQuery()
                .eq(SysFlowDef::getBizType, bizType)
                .eq(SysFlowDef::getStatus, 1)
                .orderByDesc(SysFlowDef::getDeployedVersion)
                .last("limit 1"));
    }
}
