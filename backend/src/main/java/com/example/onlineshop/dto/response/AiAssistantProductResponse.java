// src/main/java/com/example/onlineshop/dto/response/AiAssistantProductResponse.java
package com.example.onlineshop.dto.response;

import java.util.List;

public class AiAssistantProductResponse {
    private Object query;
    private String aiDescription;
    private Integer page;
    private Integer size;
    private Integer total;
    private List<ProductInfoResponse> items;

    public AiAssistantProductResponse(Object query, String aiDescription, Integer page, Integer size, Integer total, List<ProductInfoResponse> items) {
        this.query = query;
        this.aiDescription = aiDescription;
        this.page = page;
        this.size = size;
        this.total = total;
        this.items = items;
    }

    public Object getQuery() { return query; }
    public void setQuery(Object query) { this.query = query; }

    public String getAiDescription() { return aiDescription; }
    public void setAiDescription(String aiDescription) { this.aiDescription = aiDescription; }

    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }

    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }

    public Integer getTotal() { return total; }
    public void setTotal(Integer total) { this.total = total; }

    public List<ProductInfoResponse> getItems() { return items; }
    public void setItems(List<ProductInfoResponse> items) { this.items = items; }
}
