package com.guolian.pm.system.user.vo;

import com.guolian.pm.system.sensitive.annotation.Sensitive;
import com.guolian.pm.system.user.dto.UserCompanyBrief;
import com.guolian.pm.system.user.dto.UserRoleBrief;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户列表/详情出参。手机号、邮箱、身份证按当前角色脱敏策略序列化。
 */
@Data
public class UserVO {

    private Long id;
    private String username;
    private String realName;
    private String avatar;
    private String employeeNo;

    @Sensitive(group = "MOBILE", bizObject = "user")
    private String mobile;

    @Sensitive(group = "EMAIL", bizObject = "user")
    private String email;

    @Sensitive(group = "ID_CARD", bizObject = "user")
    private String idCard;

    private Integer gender;
    private String employeeType;
    private String employmentForm;
    private String position;
    private String jobLevel;
    private LocalDate joinDate;
    private LocalDate leaveDate;
    private String certStatus;
    private String certItem;
    private LocalDate certExpireDate;
    private Integer status;
    private Long defaultRoleId;
    private Long orgId;
    private String orgName;
    private String orgPath;
    private String companyNames;
    private LocalDateTime lastLoginTime;
    private String remark;
    private List<UserRoleBrief> roles = new ArrayList<>();
    private List<UserCompanyBrief> companies = new ArrayList<>();
    private List<Object> projects = new ArrayList<>();
}
