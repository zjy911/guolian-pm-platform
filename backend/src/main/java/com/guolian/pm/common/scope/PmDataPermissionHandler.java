package com.guolian.pm.common.scope;

import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import com.guolian.pm.common.web.AuthContext;
import com.guolian.pm.common.web.LoginUser;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;

import java.util.List;

/**
 * 行级数据权限拦截器：在 SQL 前拼条件。
 * 所有列表、详情、导出、统计共用同一套逻辑（都走 MyBatis-Plus 查询）。
 *
 * <p>两种表归属语义见 {@link DataScopeRegistry}：按人归属（sys_*）与按组织归属（crm_*）。
 */
public class PmDataPermissionHandler implements MultiDataPermissionHandler {

    @Override
    public Expression getSqlSegment(Table table, Expression where, String mappedStatementId) {
        LoginUser user = AuthContext.get();
        if (user == null) {
            return null;
        }
        String tableName = table.getName().toLowerCase();
        String ownerColumn = DataScopeRegistry.ownerColumn(tableName);
        String orgColumn = DataScopeRegistry.orgColumn(tableName);
        if (ownerColumn == null && orgColumn == null) {
            return null;
        }
        String scope = user.getDataScope() == null ? "SELF" : user.getDataScope();
        if ("ALL".equals(scope)) {
            return null;
        }
        String prefix = (table.getAlias() != null && table.getAlias().getName() != null)
                ? table.getAlias().getName()
                : table.getName();
        List<Long> orgIds = user.getScopeOrgIds();
        List<Long> companyIds = user.getScopeCompanyIds();

        String condition;
        if (orgColumn != null) {
            // 按组织归属的表：ORG 系列直接对 owner_org_id 过滤，SELF 对 owner_user_id 过滤
            String ownerUser = DataScopeRegistry.orgScopedOwnerColumn(tableName);
            if ("SELF".equals(scope) || "PROJECT_MEMBER".equals(scope)) {
                condition = prefix + "." + ownerUser + " = " + user.getUserId();
            } else if (orgIds != null && !orgIds.isEmpty()) {
                condition = prefix + "." + orgColumn + " in (" + join(orgIds) + ")";
            } else if (companyIds != null && !companyIds.isEmpty()) {
                // 自定义公司主体：换算成这些主体下的任职用户
                condition = prefix + "." + ownerUser + " in (select uc.user_id from sys_user_company uc"
                        + " where uc.is_deleted = 0 and uc.company_id in (" + join(companyIds) + "))";
            } else {
                condition = "1 = 0";
            }
        } else {
            String column = prefix + "." + ownerColumn;
            if ("SELF".equals(scope) || "PROJECT_MEMBER".equals(scope)) {
                condition = column + " = " + user.getUserId();
            } else if (orgIds != null && !orgIds.isEmpty()) {
                condition = column + " in (select ur.user_id from sys_user_role ur"
                        + " join sys_role r on r.id = ur.role_id"
                        + " where ur.is_deleted = 0 and r.is_deleted = 0 and r.org_id in ("
                        + join(orgIds) + "))";
            } else if (companyIds != null && !companyIds.isEmpty()) {
                condition = column + " in (select uc.user_id from sys_user_company uc"
                        + " where uc.is_deleted = 0 and uc.company_id in ("
                        + join(companyIds) + "))";
            } else {
                // 配置异常时默认不可见，绝不放开
                condition = "1 = 0";
            }
        }
        try {
            return CCJSqlParserUtil.parseCondExpression(condition);
        } catch (Exception e) {
            return null;
        }
    }

    private String join(List<Long> ids) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(ids.get(i));
        }
        return sb.toString();
    }
}
