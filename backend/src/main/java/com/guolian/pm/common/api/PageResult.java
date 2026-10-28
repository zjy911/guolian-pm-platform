package com.guolian.pm.common.api;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 统一分页响应体：data = { list, total, pageNum, pageSize }
 */
@Data
public class PageResult<T> {

    private List<T> list = new ArrayList<>();
    private long total;
    private long pageNum = 1;
    private long pageSize = 20;

    public static <T> PageResult<T> of(List<T> list, long total, long pageNum, long pageSize) {
        PageResult<T> r = new PageResult<>();
        r.list = list;
        r.total = total;
        r.pageNum = pageNum;
        r.pageSize = pageSize;
        return r;
    }

    public static <T> PageResult<T> empty(long pageNum, long pageSize) {
        return of(new ArrayList<>(), 0, pageNum, pageSize);
    }
}
