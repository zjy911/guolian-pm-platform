package com.guolian.pm.system.dict.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guolian.pm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_item")
public class SysDictItem extends BaseEntity {

    private String dictType;
    private String dictLabel;
    private String dictValue;
    private Integer sortNo;
    private Integer status;
    private String cssClass;
    private String remark;
}
