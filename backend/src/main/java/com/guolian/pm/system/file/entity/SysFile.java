package com.guolian.pm.system.file.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guolian.pm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 统一附件表，业务模块通过 fileId 引用。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_file")
public class SysFile extends BaseEntity {

    private String fileName;
    private String originalName;
    private String filePath;
    private Long fileSize;
    private String contentType;
    private String bizType;
    private Long bizId;
}
