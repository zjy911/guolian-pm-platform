package com.guolian.pm.crm.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guolian.pm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 导入批次与错误报告。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_import_batch")
public class CrmImportBatch extends BaseEntity {

    /** LEAD / ACCOUNT / CONTACT / BID */
    private String bizType;
    private Long fileId;
    private Long errorFileId;
    private Integer totalCount;
    private Integer successCount;
    private Integer failCount;
    /** PENDING / RUNNING / SUCCESS / PARTIAL / FAILED */
    private String status;
    private String errorDetail;
    private Long operatorId;
    private String remark;
}
