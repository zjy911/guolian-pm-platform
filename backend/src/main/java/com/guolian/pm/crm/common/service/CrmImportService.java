package com.guolian.pm.crm.common.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.guolian.pm.common.exception.BizException;
import com.guolian.pm.crm.account.entity.CrmAccount;
import com.guolian.pm.crm.account.mapper.CrmAccountMapper;
import com.guolian.pm.crm.bid.entity.CrmBid;
import com.guolian.pm.crm.bid.mapper.CrmBidMapper;
import com.guolian.pm.crm.common.entity.CrmImportBatch;
import com.guolian.pm.crm.common.mapper.CrmImportBatchMapper;
import com.guolian.pm.crm.contact.entity.CrmContact;
import com.guolian.pm.crm.contact.mapper.CrmContactMapper;
import com.guolian.pm.crm.lead.entity.CrmLead;
import com.guolian.pm.crm.lead.mapper.CrmLeadMapper;
import com.guolian.pm.system.file.entity.SysFile;
import com.guolian.pm.system.file.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CRM 导入：三步式（下载模板 → 上传校验 → 结果报告）。
 *
 * <p>两段式校验 + 整批原子性：任一行不通过则整批不落库，返回逐行错误报告。
 * 单次上限由参数 {@code crm.import.max.rows} 控制（默认 5000）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrmImportService {

    private final CrmImportBatchMapper batchMapper;
    private final CrmLeadMapper leadMapper;
    private final CrmAccountMapper accountMapper;
    private final CrmContactMapper contactMapper;
    private final CrmBidMapper bidMapper;
    private final CrmCryptoService cryptoService;
    private final CrmDupCheckService dupCheckService;
    private final CrmOwnerSupport ownerSupport;
    private final CrmConfig crmConfig;
    private final FileService fileService;
    private final TransactionTemplate transactionTemplate;

    /** 各业务对象的导入模板表头（顺序即列顺序） */
    public List<String> headers(String bizType) {
        return switch (bizType) {
            case "LEAD" -> List.of("姓名*", "公司名称*", "手机号*", "邮箱", "职位", "行业", "来源渠道*", "等级(A/B/C/D)");
            case "ACCOUNT" -> List.of("客户全称*", "统一社会信用代码", "机构编码", "行业*", "客户性质(TO_G/TO_B)",
                    "客户类型(KEY/NORMAL)", "省份", "城市", "详细地址", "年营业额(万元)");
            case "CONTACT" -> List.of("姓名*", "客户ID*", "手机号*", "邮箱", "职位", "部门", "生日(yyyy-MM-dd)");
            case "BID" -> List.of("招标人客户ID*", "招标项目名称*", "招标项目编号", "招标形式*", "我方投标主体ID*",
                    "省", "市", "拦标价(万元)", "开标日期(yyyy-MM-dd)*", "投标截止日期(yyyy-MM-dd)*",
                    "保证金(元)", "保证金状态", "投标安排");
            default -> throw BizException.biz("不支持的导入类型：" + bizType);
        };
    }

    /** 下载模板（首行为表头，无示例数据，避免被误当真实数据导入） */
    public byte[] template(String bizType) {
        List<List<String>> head = new ArrayList<>();
        for (String h : headers(bizType)) {
            head.add(List.of(h));
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EasyExcel.write(out).head(head).sheet("模板").doWrite(new ArrayList<List<String>>());
        return out.toByteArray();
    }

    /**
     * 导入：解析 → 逐行校验 → 全部通过才落库。
     */
    public Map<String, Object> importFile(String bizType, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BizException.biz("请选择要导入的文件");
        }
        List<Map<Integer, String>> rows = readRows(file);
        if (rows.isEmpty()) {
            throw BizException.biz("文件中没有可导入的数据行");
        }
        int max = crmConfig.importMaxRows();
        if (rows.size() > max) {
            throw BizException.biz("单次导入上限 " + max + " 行，当前 " + rows.size() + " 行");
        }
        SysFile source = fileService.upload(file, "CRM_IMPORT", 0L);

        CrmImportBatch batch = new CrmImportBatch();
        batch.setBizType(bizType);
        batch.setFileId(source.getId());
        batch.setTotalCount(rows.size());
        batch.setSuccessCount(0);
        batch.setFailCount(0);
        batch.setStatus("RUNNING");
        batch.setOperatorId(ownerSupport.currentUserId());
        batchMapper.insert(batch);

        List<String> errors = new ArrayList<>();
        // 两段式：先整体校验（不写库），全部通过后再在一个事务里整体落库
        List<Object> entities = validateAll(bizType, rows, errors);
        int created = 0;
        if (errors.isEmpty()) {
            try {
                Integer written = transactionTemplate.execute(status -> insertAll(entities));
                created = written == null ? 0 : written;
            } catch (Exception e) {
                log.error("CRM 导入落库失败 batchId={}", batch.getId(), e);
                errors.add("写库失败：" + e.getMessage());
            }
        }

        CrmImportBatch update = new CrmImportBatch();
        update.setId(batch.getId());
        update.setSuccessCount(errors.isEmpty() ? created : 0);
        update.setFailCount(errors.isEmpty() ? 0 : rows.size());
        update.setStatus(errors.isEmpty() ? "SUCCESS" : "FAILED");
        update.setErrorDetail(errors.isEmpty() ? null : String.join("\n", errors.subList(0, Math.min(errors.size(), 50))));
        batchMapper.updateById(update);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batchId", batch.getId());
        result.put("total", rows.size());
        result.put("success", update.getSuccessCount());
        result.put("fail", update.getFailCount());
        result.put("status", update.getStatus());
        result.put("errors", errors);
        return result;
    }

    public CrmImportBatch batch(Long batchId) {
        CrmImportBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw BizException.notFound("导入批次不存在");
        }
        return batch;
    }

    // ---------------- 内部 ----------------

    /**
     * 第一段：整体校验，只读不写。任一行有问题则 errors 非空，调用方整批不落库。
     */
    private List<Object> validateAll(String bizType, List<Map<Integer, String>> rows, List<String> errors) {
        List<Object> entities = new ArrayList<>();
        AtomicInteger index = new AtomicInteger(1);
        Long ownerId = ownerSupport.currentUserId();
        Long ownerOrgId = ownerSupport.createOrgId();
        String ownerName = ownerSupport.currentUserName();

        for (Map<Integer, String> row : rows) {
            int line = index.incrementAndGet();
            try {
                Object entity = switch (bizType) {
                    case "LEAD" -> buildLead(row, ownerId, ownerOrgId, ownerName, line, errors);
                    case "ACCOUNT" -> buildAccount(row, ownerId, ownerOrgId, ownerName, line, errors);
                    case "CONTACT" -> buildContact(row, ownerId, ownerOrgId, ownerName, line, errors);
                    case "BID" -> buildBid(row, ownerId, ownerOrgId, ownerName, line, errors);
                    default -> null;
                };
                if (entity != null) {
                    entities.add(entity);
                }
            } catch (BizException e) {
                errors.add("第" + line + "行：" + e.getMessage());
            } catch (Exception e) {
                errors.add("第" + line + "行：数据格式错误（" + e.getMessage() + "）");
            }
        }
        return entities;
    }

    /**
     * 第二段：整体落库（在单个事务内，失败全部回滚）。
     */
    private int insertAll(List<Object> entities) {
        for (Object entity : entities) {
            if (entity instanceof CrmLead lead) {
                leadMapper.insert(lead);
            } else if (entity instanceof CrmAccount account) {
                accountMapper.insert(account);
            } else if (entity instanceof CrmContact contact) {
                contactMapper.insert(contact);
            } else if (entity instanceof CrmBid bid) {
                bidMapper.insert(bid);
            }
        }
        return entities.size();
    }

    private CrmLead buildLead(Map<Integer, String> row, Long ownerId, Long ownerOrgId, String ownerName,
                              int line, List<String> errors) {
        String name = cell(row, 0);
        String company = cell(row, 1);
        String mobile = cell(row, 2);
        if (!StringUtils.hasText(name) || !StringUtils.hasText(company)) {
            errors.add("第" + line + "行：姓名与公司名称不能为空");
            return null;
        }
        if (mobile == null || !mobile.matches("^1\\d{10}$")) {
            errors.add("第" + line + "行：手机号格式不正确");
            return null;
        }
        if (dupCheckService.check("LEAD", mobile, cell(row, 3), null, null, null).isDuplicated()) {
            errors.add("第" + line + "行：手机号/邮箱已存在线索或客户，导入不允许强制继续");
            return null;
        }
        CrmLead lead = new CrmLead();
        lead.setName(name);
        lead.setCompanyName(company);
        lead.setMobile(cryptoService.encrypt(mobile));
        lead.setMobileHash(cryptoService.hash(mobile));
        lead.setEmail(cryptoService.encrypt(cell(row, 3)));
        lead.setEmailHash(cryptoService.hash(cell(row, 3)));
        lead.setPositionName(cell(row, 4));
        lead.setIndustry(cell(row, 5));
        lead.setSourceChannel(defaultIfBlank(cell(row, 6), "OTHER"));
        String grade = defaultIfBlank(cell(row, 7), "C").toUpperCase();
        lead.setGrade(grade.length() > 1 ? "C" : grade);
        lead.setStatus("NEW");
        lead.setOwnerUserId(ownerId);
        lead.setOwnerOrgId(ownerOrgId);
        lead.setOwnerUserName(ownerName);
        return lead;
    }

    private CrmAccount buildAccount(Map<Integer, String> row, Long ownerId, Long ownerOrgId, String ownerName,
                                    int line, List<String> errors) {
        String company = cell(row, 0);
        if (!StringUtils.hasText(company)) {
            errors.add("第" + line + "行：客户全称不能为空");
            return null;
        }
        String creditCode = cell(row, 1);
        String institutionCode = cell(row, 2);
        if (dupCheckService.check("ACCOUNT", null, null, creditCode, company, cell(row, 6)).isDuplicated()) {
            errors.add("第" + line + "行：客户已存在（信用代码或名称+省份命中）");
            return null;
        }
        CrmAccount account = new CrmAccount();
        account.setCompanyName(company);
        account.setCreditCode(creditCode);
        account.setInstitutionCode(institutionCode);
        account.setIndustry(defaultIfBlank(cell(row, 3), "OTHER"));
        account.setAccountNature(defaultIfBlank(cell(row, 4), "TO_B"));
        account.setAccountType(defaultIfBlank(cell(row, 5), "NORMAL"));
        account.setProvince(cell(row, 6));
        account.setCity(cell(row, 7));
        account.setAddress(cell(row, 8));
        String revenue = cell(row, 9);
        if (StringUtils.hasText(revenue)) {
            try {
                account.setAnnualRevenue(new BigDecimal(revenue));
            } catch (NumberFormatException e) {
                errors.add("第" + line + "行：年营业额不是合法数字");
                return null;
            }
        }
        account.setPoolStatus("PRIVATE");
        account.setIsProspect(0);
        account.setOwnerUserId(ownerId);
        account.setOwnerOrgId(ownerOrgId);
        account.setOwnerUserName(ownerName);
        return account;
    }

    private CrmContact buildContact(Map<Integer, String> row, Long ownerId, Long ownerOrgId, String ownerName,
                                    int line, List<String> errors) {
        String name = cell(row, 0);
        String accountId = cell(row, 1);
        String mobile = cell(row, 2);
        if (!StringUtils.hasText(name) || !StringUtils.hasText(accountId)) {
            errors.add("第" + line + "行：姓名与客户ID不能为空");
            return null;
        }
        if (mobile == null || !mobile.matches("^1\\d{10}$")) {
            errors.add("第" + line + "行：手机号格式不正确");
            return null;
        }
        CrmContact contact = new CrmContact();
        contact.setName(name);
        contact.setAccountId(Long.valueOf(accountId.trim()));
        contact.setMobile(cryptoService.encrypt(mobile));
        contact.setMobileHash(cryptoService.hash(mobile));
        contact.setEmail(cryptoService.encrypt(cell(row, 3)));
        contact.setEmailHash(cryptoService.hash(cell(row, 3)));
        contact.setPositionName(cell(row, 4));
        contact.setDepartment(cell(row, 5));
        String birthday = cell(row, 6);
        if (StringUtils.hasText(birthday)) {
            try {
                contact.setBirthday(LocalDate.parse(birthday.trim()));
            } catch (Exception e) {
                errors.add("第" + line + "行：生日格式应为 yyyy-MM-dd");
                return null;
            }
        }
        contact.setOwnerUserId(ownerId);
        contact.setOwnerOrgId(ownerOrgId);
        contact.setOwnerUserName(ownerName);
        return contact;
    }

    private CrmBid buildBid(Map<Integer, String> row, Long ownerId, Long ownerOrgId, String ownerName,
                            int line, List<String> errors) {
        String accountId = cell(row, 0);
        String tenderName = cell(row, 1);
        String tenderForm = cell(row, 3);
        String bidEntity = cell(row, 4);
        String openDate = cell(row, 8);
        String deadline = cell(row, 9);
        if (!StringUtils.hasText(accountId) || !StringUtils.hasText(tenderName)
                || !StringUtils.hasText(bidEntity)) {
            errors.add("第" + line + "行：招标人客户ID、招标项目名称、投标主体ID 不能为空");
            return null;
        }
        CrmBid bid = new CrmBid();
        bid.setAccountId(Long.valueOf(accountId.trim()));
        bid.setTenderName(tenderName);
        bid.setTenderNo(cell(row, 2));
        bid.setTenderForm(defaultIfBlank(tenderForm, "OTHER"));
        bid.setBidEntity(Long.valueOf(bidEntity.trim()));
        bid.setProvince(cell(row, 5));
        bid.setCity(cell(row, 6));
        bid.setPriceCap(decimalOrNull(cell(row, 7), "拦标价", line, errors));
        try {
            bid.setOpenDate(LocalDate.parse(openDate == null ? "" : openDate.trim()));
            bid.setDeadline(LocalDate.parse(deadline == null ? "" : deadline.trim()));
        } catch (Exception e) {
            errors.add("第" + line + "行：开标日期与截止日期必填，格式 yyyy-MM-dd");
            return null;
        }
        bid.setDeposit(decimalOrNull(cell(row, 10), "保证金", line, errors));
        bid.setDepositStatus(cell(row, 11));
        bid.setBidPlan(cell(row, 12));
        bid.setBidStatus("TRACKING");
        bid.setIsWon(0);
        bid.setOwnerUserId(ownerId);
        bid.setOwnerOrgId(ownerOrgId);
        bid.setOwnerUserName(ownerName);
        bid.setRegionOrgId(ownerOrgId);
        return bid;
    }

    private BigDecimal decimalOrNull(String value, String label, int line, List<String> errors) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            errors.add("第" + line + "行：" + label + "不是合法数字");
            return null;
        }
    }

    private List<Map<Integer, String>> readRows(MultipartFile file) {
        List<Map<Integer, String>> rows = new ArrayList<>();
        try (InputStream in = file.getInputStream()) {
            EasyExcel.read(in, new AnalysisEventListener<Map<Integer, String>>() {
                @Override
                public void invoke(Map<Integer, String> data, AnalysisContext context) {
                    boolean empty = data.values().stream().allMatch(v -> !StringUtils.hasText(v));
                    if (!empty) {
                        rows.add(data);
                    }
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    // 解析完成，无需额外处理
                }
            }).sheet().headRowNumber(1).doRead();
        } catch (IOException e) {
            throw BizException.biz("文件读取失败：" + e.getMessage());
        }
        return rows;
    }

    private String cell(Map<Integer, String> row, int index) {
        String value = row.get(index);
        return value == null ? null : value.trim();
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}
