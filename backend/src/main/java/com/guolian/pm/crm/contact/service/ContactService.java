package com.guolian.pm.crm.contact.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guolian.pm.common.api.PageResult;
import com.guolian.pm.common.exception.BizException;
import com.guolian.pm.crm.account.entity.CrmAccount;
import com.guolian.pm.crm.account.mapper.CrmAccountMapper;
import com.guolian.pm.crm.common.dto.CrmQuery;
import com.guolian.pm.crm.common.service.CrmCryptoService;
import com.guolian.pm.crm.common.service.CrmOwnerSupport;
import com.guolian.pm.crm.contact.entity.CrmContact;
import com.guolian.pm.crm.contact.mapper.CrmContactMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 联系人服务：必属一个客户；同客户下「同名 + 同手机号」重复阻止，跨客户仅提示。
 */
@Service
@RequiredArgsConstructor
public class ContactService {

    private final CrmContactMapper contactMapper;
    private final CrmAccountMapper accountMapper;
    private final CrmCryptoService cryptoService;
    private final CrmOwnerSupport ownerSupport;

    public PageResult<CrmContact> page(CrmQuery q) {
        Page<CrmContact> page = new Page<>(q.safePageNum(), q.safePageSize());
        var wrapper = Wrappers.<CrmContact>lambdaQuery()
                .eq(q.getAccountId() != null, CrmContact::getAccountId, q.getAccountId())
                .eq(q.getOwnerUserId() != null, CrmContact::getOwnerUserId, q.getOwnerUserId())
                .like(StringUtils.hasText(q.getKeyword()), CrmContact::getName, q.getKeyword())
                .orderByDesc(CrmContact::getCreatedTime);
        Page<CrmContact> result = contactMapper.selectPage(page, wrapper);
        result.getRecords().forEach(this::decryptInPlace);
        return PageResult.of(result.getRecords(), result.getTotal(), q.safePageNum(), q.safePageSize());
    }

    public CrmContact get(Long id) {
        CrmContact contact = contactMapper.selectById(id);
        if (contact == null) {
            throw BizException.notFound("联系人不存在或无权查看");
        }
        decryptInPlace(contact);
        return contact;
    }

    @Transactional
    public Long create(CrmContact request) {
        if (!StringUtils.hasText(request.getName())) {
            throw BizException.biz("请填写联系人姓名");
        }
        if (request.getAccountId() == null) {
            throw BizException.biz("联系人必须归属一个客户");
        }
        if (!StringUtils.hasText(request.getMobile()) || !request.getMobile().matches("^1\\d{10}$")) {
            throw BizException.biz("手机号格式不正确（需 11 位）");
        }
        CrmAccount account = accountMapper.selectById(request.getAccountId());
        if (account == null) {
            throw BizException.notFound("所属客户不存在或无权维护");
        }
        String hash = cryptoService.hash(request.getMobile());
        if (contactMapper.countSameInAccount(request.getAccountId(), request.getName(), hash) > 0) {
            throw BizException.biz("该客户下已存在同名同手机号的联系人，请勿重复录入");
        }
        request.setId(null);
        request.setMobileHash(hash);
        request.setEmailHash(cryptoService.hash(request.getEmail()));
        request.setMobile(cryptoService.encrypt(request.getMobile()));
        request.setEmail(cryptoService.encrypt(request.getEmail()));
        request.setOwnerUserId(ownerSupport.currentUserId());
        request.setOwnerOrgId(ownerSupport.createOrgId());
        request.setOwnerUserName(ownerSupport.currentUserName());
        contactMapper.insert(request);
        return request.getId();
    }

    @Transactional
    public void update(Long id, CrmContact request) {
        CrmContact exist = contactMapper.selectById(id);
        if (exist == null) {
            throw BizException.notFound("联系人不存在或无权修改");
        }
        request.setId(id);
        request.setAccountId(exist.getAccountId());
        request.setOwnerUserId(exist.getOwnerUserId());
        request.setOwnerOrgId(exist.getOwnerOrgId());
        request.setOwnerUserName(exist.getOwnerUserName());
        if (StringUtils.hasText(request.getMobile())) {
            request.setMobileHash(cryptoService.hash(request.getMobile()));
            request.setMobile(cryptoService.encrypt(request.getMobile()));
        }
        if (StringUtils.hasText(request.getEmail())) {
            request.setEmailHash(cryptoService.hash(request.getEmail()));
            request.setEmail(cryptoService.encrypt(request.getEmail()));
        }
        contactMapper.updateById(request);
    }

    @Transactional
    public void delete(Long id) {
        CrmContact contact = contactMapper.selectById(id);
        if (contact == null) {
            throw BizException.notFound("联系人不存在或无权删除");
        }
        contactMapper.deleteById(id);
        // 主要联系人被删时清空客户上的引用
        CrmAccount account = accountMapper.selectById(contact.getAccountId());
        if (account != null && id.equals(account.getPrimaryContactId())) {
            CrmAccount update = new CrmAccount();
            update.setId(account.getId());
            update.setPrimaryContactId(null);
            accountMapper.updateById(update);
        }
    }

    /** 设为主要联系人：必须属于本客户 */
    @Transactional
    public void setPrimary(Long id) {
        CrmContact contact = contactMapper.selectById(id);
        if (contact == null) {
            throw BizException.notFound("联系人不存在或无权操作");
        }
        CrmAccount update = new CrmAccount();
        update.setId(contact.getAccountId());
        update.setPrimaryContactId(id);
        accountMapper.updateById(update);
    }

    private void decryptInPlace(CrmContact contact) {
        contact.setMobile(cryptoService.decrypt(contact.getMobile()));
        contact.setEmail(cryptoService.decrypt(contact.getEmail()));
        contact.setMobileHash(null);
        contact.setEmailHash(null);
    }
}
