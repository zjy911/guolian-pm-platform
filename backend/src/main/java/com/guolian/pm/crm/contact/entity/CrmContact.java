package com.guolian.pm.crm.contact.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guolian.pm.common.entity.BaseEntity;
import com.guolian.pm.system.sensitive.annotation.Sensitive;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 联系人：客户侧关键人。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_contact")
public class CrmContact extends BaseEntity {

    private String name;
    private Long accountId;

    @Sensitive(group = "MOBILE", bizObject = "contact")
    private String mobile;

    private String mobileHash;

    @Sensitive(group = "EMAIL", bizObject = "contact")
    private String email;

    private String emailHash;
    private String positionName;
    private String department;
    private LocalDate birthday;

    @Sensitive(group = "ADDRESS", bizObject = "contact")
    private String address;

    private Long ownerUserId;
    private Long ownerOrgId;
    private String ownerUserName;
    private LocalDateTime lastActivityTime;
    private String remark;
}
