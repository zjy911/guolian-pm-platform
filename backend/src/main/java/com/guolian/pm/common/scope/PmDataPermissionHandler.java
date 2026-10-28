package com.guolian.pm.common.scope;

import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import com.guolian.pm.common.web.AuthContext;
import com.guolian.pm.common.web.LoginUser;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;

/**
 * 行级数据权限拦截器：在 SQL 前拼条件。
 * 所有列表、详情、导出、统计共用同一套逻辑（都走 MyBatis-Plus 查询）。
 */
public class PmDataPermissionHandler implements MultiDataPermissionHandler {

    @Override
    public Expression getSqlSegment(Table table, Expression where, String mappedStatementId) {
        LoginUser user = AuthContext.get();
        if (user == null) {
            return null;
        }
        String tableName = table.getName().toLowerCase();
        String owner = DataScopeRegistry.ownerColumn(tableName);
        if (owner == null) {
            return null;
        }
        String scope = user.getDataScope() == null ? "SELF" : user.getDataScope();
        if ("ALL".equals(scope)) {
            return null;
        }
        String prefix = (table.getAlias() != null && table.getAlias().getName() != null)
                ? table.getAlias().getName()
                : table.getName();
        String column = prefix + "." + owner;

        String condition;
        if ("SELF".equals(scope) || "PROJECT_MEMBER".equals(scope)) {
            condition = column + " = " + user.getUserId();
        } else if (user.getScopeOrgIds() != null && !user.getScopeOrgIds().isEmpty()) {
            condition = column + " in (select ur.user_id from sys_user_role ur"
                    + " join sys_role r on r.id = ur.role_id"
                    + " where ur.is_deleted = 0 and r.is_deleted = 0 and r.org_id in ("
                    + join(user.getScopeOrgIds()) + "))";
        } else if (user.getScopeCompanyIds() != null && !user.getScopeCompanyIds().isEmpty()) {
            condition = column + " in (select uc.user_id from sys_user_company uc"
                    + " where uc.is_deleted = 0 and uc.company_id in ("
                    + join(user.getScopeCompanyIds()) + "))";
        } else {
            // 配置异常时默认不可见，绝不放开
            condition = "1 = 0";
        }
        try {
            return CCJSqlParserUtil.parseCondExpression(condition);
        } catch (Exception e) {
            return null;
        }
    }

    private String join(java.util.List<Long> ids) {
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
