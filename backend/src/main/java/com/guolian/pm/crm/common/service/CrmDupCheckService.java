package com.guolian.pm.crm.common.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guolian.pm.crm.account.entity.CrmAccount;
import com.guolian.pm.crm.account.mapper.CrmAccountMapper;
import com.guolian.pm.crm.common.dto.CrmDtos;
import com.guolian.pm.crm.contact.entity.CrmContact;
import com.guolian.pm.crm.contact.mapper.CrmContactMapper;
import com.guolian.pm.crm.lead.entity.CrmLead;
import com.guolian.pm.crm.lead.mapper.CrmLeadMapper;
import com.guolian.pm.system.sensitive.service.MaskUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 查重服务：线索 / 客户 / 联系人共用，接口与导入两条路必须走同一实现。
 *
 * <p>因允许「强制继续」（填 dup_reason 留痕），手机号与邮箱<b>不建唯一索引</b>，
 * 一律应用层查重；查重走 HMAC 哈希，命中后手机号按脱敏算法返回摘要，绝不回明文。
 */
@Service
@RequiredArgsConstructor
public class CrmDupCheckService {

    private final CrmLeadMapper leadMapper;
    private final CrmAccountMapper accountMapper;
    private final CrmContactMapper contactMapper;
    private final CrmCryptoService cryptoService;

    /**
     * @param type        LEAD / ACCOUNT / CONTACT，null 表示不限
     * @param mobile      手机号明文（服务端入参，不回显）
     * @param email       邮箱明文
     * @param creditCode  统一社会信用代码
     * @param companyName 公司名称（客户无信用代码时按名称 + 省份查）
     * @param province    省份
     */
    public CrmDtos.DupCheckResult check(String type, String mobile, String email,
                                        String creditCode, String companyName, String province) {
        CrmDtos.DupCheckResult result = new CrmDtos.DupCheckResult();
        List<CrmDtos.DupHit> hits = new ArrayList<>();

        boolean wantLead = type == null || "LEAD".equalsIgnoreCase(type) || "CONTACT".equalsIgnoreCase(type);
        boolean wantAccount = type == null || "ACCOUNT".equalsIgnoreCase(type);

        if (wantLead && StringUtils.hasText(mobile)) {
            String hash = cryptoService.hash(mobile);
            for (Long id : leadMapper.selectIdsByMobileHash(hash)) {
                CrmLead lead = leadMapper.selectById(id);
                if (lead != null) {
                    hits.add(leadHit(lead));
                }
            }
            for (CrmContact c : contactMapper.selectList(Wrappers.<CrmContact>lambdaQuery()
                    .eq(CrmContact::getMobileHash, hash).last("limit 5"))) {
                hits.add(hit("CONTACT", c.getId(), c.getName(), null, c.getOwnerUserName(), null, c.getMobile()));
            }
        }
        if (wantLead && StringUtils.hasText(email)) {
            String hash = cryptoService.hash(email);
            for (Long id : leadMapper.selectIdsByEmailHash(hash)) {
                CrmLead lead = leadMapper.selectById(id);
                boolean exists = hits.stream().anyMatch(h -> "LEAD".equals(h.getType()) && h.getId().equals(id));
                if (lead != null && !exists) {
                    hits.add(leadHit(lead));
                }
            }
        }
        if (wantAccount && StringUtils.hasText(creditCode)) {
            for (Long id : accountMapper.selectIdsByCreditCode(creditCode)) {
                CrmAccount account = accountMapper.selectById(id);
                if (account != null) {
                    hits.add(accountHit(account));
                }
            }
        }
        if (wantAccount && !StringUtils.hasText(creditCode) && StringUtils.hasText(companyName)) {
            List<CrmAccount> sameName = accountMapper.selectList(Wrappers.<CrmAccount>lambdaQuery()
                    .eq(CrmAccount::getCompanyName, companyName)
                    .eq(StringUtils.hasText(province), CrmAccount::getProvince, province)
                    .last("limit 5"));
            for (CrmAccount account : sameName) {
                hits.add(accountHit(account));
            }
        }
        result.setHits(hits);
        result.setDuplicated(!hits.isEmpty());
        return result;
    }

    private CrmDtos.DupHit leadHit(CrmLead lead) {
        return hit("LEAD", lead.getId(), lead.getName(), lead.getCompanyName(),
                lead.getOwnerUserName(), null, lead.getMobile());
    }

    private CrmDtos.DupHit accountHit(CrmAccount account) {
        return hit("ACCOUNT", account.getId(), account.getCompanyName(), account.getCompanyName(),
                account.getOwnerUserName(), account.getPoolStatus(), account.getPhone());
    }

    private CrmDtos.DupHit hit(String type, Long id, String name, String companyName,
                              String ownerName, String poolStatus, String mobileCipher) {
        CrmDtos.DupHit h = new CrmDtos.DupHit();
        h.setType(type);
        h.setId(id);
        h.setName(name);
        h.setCompanyName(companyName);
        h.setOwnerUserName(ownerName);
        h.setPoolStatus(poolStatus);
        // 摘要里的手机号同样按脱敏算法展示，不返回明文
        String plain = cryptoService.decrypt(mobileCipher);
        h.setMobileMasked(StringUtils.hasText(plain) ? MaskUtil.apply("KEEP_HEAD_TAIL", null, plain) : null);
        return h;
    }
}
