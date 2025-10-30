// java
package com.example.onlineshop.dto.response;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CategoryResponse {
    private Integer categoryId;
    private Integer parentId;
    private String categoryName;
    private Integer categoryLevel;
    private LocalDateTime createdAt;

    private List<CategoryResponse> children = new ArrayList<>();

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
    public Integer getParentId() { return parentId; }
    public void setParentId(Integer parentId) { this.parentId = parentId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public Integer getCategoryLevel() { return categoryLevel; }
    public void setCategoryLevel(Integer categoryLevel) { this.categoryLevel = categoryLevel; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public List<CategoryResponse> getChildren() { return children; }
    public void setChildren(List<CategoryResponse> children) { this.children = children; }
}
