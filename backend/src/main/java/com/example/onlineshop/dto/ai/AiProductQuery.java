// src/main/java/com/example/onlineshop/dto/ai/AiProductQuery.java
package com.example.onlineshop.dto.ai;

import java.math.BigDecimal;

public class AiProductQuery {
    private String q;
    private Integer categoryId;
    private String status;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String sortBy;
    private String order;
    private Integer page;
    private Integer size;

    public String getQ() { return q; }
    public void setQ(String q) { this.q = q; }

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }

    public BigDecimal getMaxPrice() { return maxPrice; }
    public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }

    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }

    public String getOrder() { return order; }
    public void setOrder(String order) { this.order = order; }

    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }

    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
}
