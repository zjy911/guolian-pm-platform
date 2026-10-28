package com.guolian.pm.crm.account.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guolian.pm.crm.account.entity.CrmClaimApply;
import com.guolian.pm.crm.account.vo.CrmClaimVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmClaimApplyMapper extends BaseMapper<CrmClaimApply> {

    /**
     * 认领申请分页。crm_claim_apply 未注册行级权限（审批视角需要看跨组织申请），
     * 访问入口由 crm:account:claim 权限位把关。
     */
    @InterceptorIgnore(dataPermission = "1")
    @Select("<script>"
            + "select a.id, a.account_id, a.applicant_id, a.applicant_org_id, a.old_owner_id, a.status, "
            + "a.flow_instance_id, a.appeal_deadline, a.appeal_status, a.apply_reason, a.remark, "
            + "a.created_time, acc.company_name as account_name, acc.pool_status as pool_status, "
            + "u.real_name as applicant_name, o.real_name as old_owner_name "
            + "from crm_claim_apply a "
            + "left join crm_account acc on acc.id = a.account_id "
            + "left join sys_user u on u.id = a.applicant_id "
            + "left join sys_user o on o.id = a.old_owner_id "
            + "where a.is_deleted = 0 "
            + "<if test='status != null and status != \"\"'>and a.status = #{status} </if>"
            + "<if test='keyword != null and keyword != \"\"'>"
            + "and (acc.company_name like concat('%', #{keyword}, '%') "
            + "or u.real_name like concat('%', #{keyword}, '%')) </if>"
            + "order by a.created_time desc limit #{limit} offset #{offset}"
            + "</script>")
    List<CrmClaimVO> selectClaimPage(@Param("status") String status, @Param("keyword") String keyword,
                                     @Param("offset") int offset, @Param("limit") int limit);

    @InterceptorIgnore(dataPermission = "1")
    @Select("<script>"
            + "select count(1) from crm_claim_apply a "
            + "left join crm_account acc on acc.id = a.account_id "
            + "left join sys_user u on u.id = a.applicant_id "
            + "where a.is_deleted = 0 "
            + "<if test='status != null and status != \"\"'>and a.status = #{status} </if>"
            + "<if test='keyword != null and keyword != \"\"'>"
            + "and (acc.company_name like concat('%', #{keyword}, '%') "
            + "or u.real_name like concat('%', #{keyword}, '%')) </if>"
            + "</script>")
    long countClaim(@Param("status") String status, @Param("keyword") String keyword);

    /** 待审批数量：菜单角标用 */
    @InterceptorIgnore(dataPermission = "1")
    @Select("select count(1) from crm_claim_apply where is_deleted = 0 and status = 'APPROVING'")
    long countApproving();
}
