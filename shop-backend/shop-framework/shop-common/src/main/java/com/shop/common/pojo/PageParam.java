package com.shop.common.pojo;

import lombok.Data;

import java.io.Serializable;

@Data
public class PageParam implements Serializable {
    public static final int MAX_PAGE_SIZE = 100;

    private Integer pageNo = 1;
    private Integer pageSize = 10;

    public Integer getPageNo() {
        return pageNo == null ? 1 : Math.max(pageNo, 1);
    }

    public Integer getPageSize() {
        return pageSize == null ? 10 : Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
    }
}
