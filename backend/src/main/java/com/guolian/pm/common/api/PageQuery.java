package com.guolian.pm.common.api;

import lombok.Data;

/**
 * 分页入参基类，限制最大页大小，避免一次性拉全表。
 */
@Data
public class PageQuery {

    private long pageNum = 1;
    private long pageSize = 20;

    public long safePageNum() {
        return pageNum < 1 ? 1 : pageNum;
    }

    public long safePageSize() {
        if (pageSize < 1) {
            return 20;
        }
        return Math.min(pageSize, 100);
    }
}
