package com.guolian.pm.pms.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guolian.pm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 任务模板：6 环节任务定义（JSON），发布后版本只增不改；项目立项取快照（PRD 4.3）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pm_task_template")
public class PmTaskTemplate extends BaseEntity {

    private Long productId;
    private Integer version;
    private String templateJson;
    private String status;
    private LocalDateTime publishTime;
}
