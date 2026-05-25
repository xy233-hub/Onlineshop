// src/main/java/com/example/onlineshop/dto/request/AiAssistantQueryRequest.java
package com.example.onlineshop.dto.request;

import java.util.List;

public class AiAssistantQueryRequest {
    private String text;
    private Integer page;
    private Integer size;
    private String userId;
    private String action;
    private String scene;
    private Integer sessionId;
    private Integer productId;
    private List<Integer> candidateProductIds;

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }

    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getScene() { return scene; }
    public void setScene(String scene) { this.scene = scene; }

    public Integer getSessionId() { return sessionId; }
    public void setSessionId(Integer sessionId) { this.sessionId = sessionId; }

    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }

    public List<Integer> getCandidateProductIds() { return candidateProductIds; }
    public void setCandidateProductIds(List<Integer> candidateProductIds) { this.candidateProductIds = candidateProductIds; }
}
