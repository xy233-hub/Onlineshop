// java
package com.example.onlineshop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CategoryRequest {
    private Integer parentId;

    @NotBlank(message = "categoryName 必填")
    @Size(min = 1, max = 64, message = "categoryName 长度 1-64")
    private String categoryName;

    public Integer getParentId() {
        return parentId;
    }
    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }
    public String getCategoryName() {
        return categoryName;
    }
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
}

