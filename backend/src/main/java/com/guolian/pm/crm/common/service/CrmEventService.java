package com.guolian.pm.crm.common.service;

import com.guolian.pm.crm.common.entity.CrmEventLog;
import com.guolian.pm.crm.common.mapper.CrmEventLogMapper;
import com.guolian.pm.common.web.AuthContext;
import com.guolian.pm.common.web.LoginUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CRM 埋点服务（服务端上报）。
 *
 * <p>底座未提供独立埋点中间件，按方案要求落 {@code crm_event_log} 并声明为临时方案；
 * 后续接入统一埋点服务时只替换本类实现，调用方不变。埋点一律服务端写，禁止前端直报。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrmEventService {

    public static final String LEAD_CREATE = "crm_lead_create";
    public static final String LEAD_CONVERT = "crm_lead_convert";
    public static final String OPP_STAGE_CHANGE = "crm_opp_stage_change";
    public static final String CONTRACT_ACTIVATE = "crm_contract_activate";
    public static final String BID_STATUS_CHANGE = "crm_bid_status_change";

    private final CrmEventLogMapper eventLogMapper;
    private final ObjectMapper objectMapper;

    public void track(String eventCode, Long bizId, Map<String, Object> props) {
        try {
            CrmEventLog log = new CrmEventLog();
            log.setEventCode(eventCode);
            log.setBizId(bizId);
            LoginUser user = AuthContext.get();
            if (user != null) {
                log.setOperatorId(user.getUserId());
                log.setOperatorOrgId(user.getOrgId());
            }
            log.setProps(props == null ? null : objectMapper.writeValueAsString(props));
            log.setCreatedTime(LocalDateTime.now());
            log.setIsDeleted(0);
            eventLogMapper.insert(log);
        } catch (Exception e) {
            // 埋点失败不得影响主流程
            log.warn("crm event track failed: {}", eventCode, e);
        }
    }

    public static Map<String, Object> props(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            map.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return map;
    }
}
