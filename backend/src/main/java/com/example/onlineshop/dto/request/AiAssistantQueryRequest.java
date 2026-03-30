// src/main/java/com/example/onlineshop/dto/request/AiAssistantQueryRequest.java
package com.example.onlineshop.dto.request;

public class AiAssistantQueryRequest {
    private String text;
    private Integer page;
    private Integer size;

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }

    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
}
