package com.silverline.erp.common.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PagedResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;

    public PagedResponse(Page<T> pageInfo) {
        this.content = pageInfo.getContent();
        this.page = pageInfo.getNumber();
        this.size = pageInfo.getSize();
        this.totalElements = pageInfo.getTotalElements();
        this.totalPages = pageInfo.getTotalPages();
        this.last = pageInfo.isLast();
    }

    public static <T> PagedResponse<T> from(Page<T> pageInfo) {
        return new PagedResponse<>(pageInfo);
    }
}
