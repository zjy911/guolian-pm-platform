package com.guolian.pm.crm.common.job;

import com.guolian.pm.crm.account.service.AccountService;
import com.guolian.pm.crm.activity.service.ActivityService;
import com.guolian.pm.crm.agreement.service.AgreementService;
import com.guolian.pm.crm.bid.service.BidService;
import com.guolian.pm.crm.contract.service.ContractService;
import com.guolian.pm.crm.lead.service.LeadService;
import com.guolian.pm.system.job.entity.SysJob;
import com.guolian.pm.system.job.mapper.SysJobMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * CRM 后台任务调度器。
 *
 * <p>设计要点：
 * <ul>
 *   <li>调度由 Spring {@code @Scheduled} 驱动，开关与展示由 {@code sys_job} 承载——
 *       运维可以在表里停用某个任务（status=0），不用改代码重启；</li>
 *   <li>每个任务自身幂等（条件更新 / 待办去重），重复执行不会造成脏数据；</li>
 *   <li>单个任务异常只记录日志不抛出，避免一个任务挂掉拖垮其余任务；</li>
 *   <li>每次执行成功回写 {@code last_exec_time}，供后台监控查看。</li>
 * </ul></p>
 *
 * <p>生产环境建议把本类拆成独立进程（带分布式锁），避免多实例重复执行。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrmJobScheduler {

    private final SysJobMapper jobMapper;
    private final AccountService accountService;
    private final LeadService leadService;
    private final ContractService contractService;
    private final AgreementService agreementService;
    private final BidService bidService;
    private final ActivityService activityService;

    /** 客户池自动回收：无活动超期的私有客户入公共池 */
    @Scheduled(cron = "0 30 2 * * ?")
    public void accountPoolRecycle() {
        run("CRM_ACCOUNT_POOL_RECYCLE", () -> {
            int n = accountService.autoRecycle();
            return n + " 个客户移入公共池";
        });
    }

    /** 线索未跟进回收：超期 NEW 线索置无效 */
    @Scheduled(cron = "0 0 3 * * ?")
    public void leadRecycle() {
        run("CRM_LEAD_RECYCLE", () -> {
            int n = leadService.recycleTimeoutLeads();
            return n + " 条线索置无效";
        });
    }

    /** 合同到期扫描：先提醒，再置到期 */
    @Scheduled(cron = "0 0 8 * * ?")
    public void contractExpire() {
        run("CRM_CONTRACT_EXPIRE", () -> {
            int reminded = contractService.remindExpiring();
            int expired = contractService.expireContracts();
            return "提醒 " + reminded + " 条，置到期 " + expired + " 条";
        });
    }

    /** 协议到期扫描：长协 end_date 为空自动跳过（SQL 已带条件） */
    @Scheduled(cron = "0 10 8 * * ?")
    public void agreementExpire() {
        run("CRM_AGREEMENT_EXPIRE", () -> {
            int n = agreementService.expireAgreements();
            return n + " 份协议置到期";
        });
    }

    /** 投标开标提醒 */
    @Scheduled(cron = "0 20 8 * * ?")
    public void bidOpeningRemind() {
        run("CRM_BID_OPENING_REMIND", () -> {
            int n = bidService.remindOpeningSoon();
            return "推送 " + n + " 条开标待办";
        });
    }

    /** 跟进任务逾期提醒 */
    @Scheduled(cron = "0 40 8 * * ?")
    public void activityOverdue() {
        run("CRM_ACTIVITY_OVERDUE", () -> {
            int n = activityService.remindOverdueTasks();
            return "推送 " + n + " 条逾期待办";
        });
    }

    /** 认领申诉超时处理 */
    @Scheduled(cron = "0 0 9 * * ?")
    public void claimAppealTimeout() {
        run("CRM_CLAIM_APPEAL_TIMEOUT", () -> {
            int n = accountService.processAppealTimeout();
            return n + " 条申诉超时关闭";
        });
    }

    /**
     * 统一执行外壳：读开关 → 执行 → 回写执行时间。
     * 任务未在 sys_job 中登记时按「启用」处理（代码已发布但没配数据不该静默不跑）。
     */
    private void run(String jobCode, IntSupplierTask task) {
        SysJob job = jobMapper.selectOne(com.baomidou.mybatisplus.core.toolkit.Wrappers.<SysJob>lambdaQuery()
                .eq(SysJob::getJobCode, jobCode)
                .last("limit 1"));
        if (job != null && job.getStatus() != null && job.getStatus() == 0) {
            log.debug("定时任务 {} 已停用，跳过", jobCode);
            return;
        }
        long start = System.currentTimeMillis();
        try {
            String result = task.run();
            log.info("定时任务 {} 执行完成：{}，耗时 {}ms", jobCode, result, System.currentTimeMillis() - start);
            if (job != null) {
                SysJob update = new SysJob();
                update.setId(job.getId());
                update.setLastExecTime(LocalDateTime.now());
                jobMapper.updateById(update);
            }
        } catch (Exception ex) {
            // 单任务失败不影响其它任务，异常进日志由告警消费
            log.error("定时任务 {} 执行失败，耗时 {}ms", jobCode, System.currentTimeMillis() - start, ex);
        }
    }

    /** 允许返回执行结果描述，便于日志审计 */
    @FunctionalInterface
    public interface IntSupplierTask {
        String run();
    }
}
