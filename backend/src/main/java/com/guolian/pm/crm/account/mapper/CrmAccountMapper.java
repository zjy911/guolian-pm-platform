package com.guolian.pm.crm.account.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guolian.pm.crm.account.entity.CrmAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CrmAccountMapper extends BaseMapper<CrmAccount> {

    /**
     * 公共池客户：显式豁免行级权限（有 crm:account:pool 权限者全员可见，只读 + 脱敏）。
     * 豁免只在本方法声明，不写进任何业务 SQL。
     */
    @InterceptorIgnore(dataPermission = "1")
    @Select("<script>select * from crm_account where is_deleted = 0 and pool_status = 'POOL' "
            + "and is_prospect = 0 "
            + "<if test='keyword != null and keyword != \"\"'>and company_name like concat('%', #{keyword}, '%') </if>"
            + "order by pool_in_time desc limit #{limit} offset #{offset}</script>")
    List<CrmAccount> selectPoolPage(@Param("keyword") String keyword, @Param("offset") int offset,
                                    @Param("limit") int limit);

    @InterceptorIgnore(dataPermission = "1")
    @Select("<script>select count(1) from crm_account where is_deleted = 0 and pool_status = 'POOL' "
            + "and is_prospect = 0 "
            + "<if test='keyword != null and keyword != \"\"'>and company_name like concat('%', #{keyword}, '%') </if>"
            + "</script>")
    long countPool(@Param("keyword") String keyword);

    /** 查重：信用代码全局唯一，必须跨范围查 */
    @InterceptorIgnore(dataPermission = "1")
    @Select("select id from crm_account where credit_code = #{creditCode} and is_deleted = 0 limit 5")
    List<Long> selectIdsByCreditCode(@Param("creditCode") String creditCode);

    @InterceptorIgnore(dataPermission = "1")
    @Select("select id from crm_account where institution_code = #{code} and is_deleted = 0 limit 5")
    List<Long> selectIdsByInstitutionCode(@Param("code") String code);

    /** 交接：公共池客户不参与交接，计数与执行都必须排除 */
    @InterceptorIgnore(dataPermission = "1")
    @Select("select id from crm_account where owner_user_id = #{uid} and is_deleted = 0 "
            + "and pool_status <> 'POOL' order by id limit #{limit} offset #{offset}")
    List<Long> selectTransferIds(@Param("uid") Long uid, @Param("offset") int offset, @Param("limit") int limit);

    @InterceptorIgnore(dataPermission = "1")
    @Select("select count(1) from crm_account where owner_user_id = #{uid} and is_deleted = 0 "
            + "and pool_status <> 'POOL'")
    long countTransfer(@Param("uid") Long uid);

    /** 删除前置引用检查（跨范围，因为引用方也是本模块数据） */
    @InterceptorIgnore(dataPermission = "1")
    @Select("select (select count(1) from crm_opportunity where account_id = #{id} and is_deleted = 0) "
            + "+ (select count(1) from crm_contract where account_id = #{id} and is_deleted = 0) "
            + "+ (select count(1) from crm_agreement where account_id = #{id} and is_deleted = 0) "
            + "+ (select count(1) from crm_bid where account_id = #{id} and is_deleted = 0) "
            + "+ (select count(1) from crm_contact where account_id = #{id} and is_deleted = 0)")
    long countReferences(@Param("id") Long id);

    /** 超期回收：全库扫描，按最近活动时间 */
    @InterceptorIgnore(dataPermission = "1")
    @Select("select id from crm_account where is_deleted = 0 and pool_status = 'PRIVATE' "
            + "and (last_activity_time is null or last_activity_time < #{before}) limit #{limit}")
    List<Long> selectTimeoutIds(@Param("before") java.time.LocalDateTime before, @Param("limit") int limit);
}
