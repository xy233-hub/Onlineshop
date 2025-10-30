// file: backend/src/main/java/com/example/onlineshop/entity/Product.java
package com.example.onlineshop.entity;

import lombok.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品主表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer productId;
    private Integer sellerId;
    private Integer categoryId;
    private String productName;
    private String productDesc;
    private BigDecimal price;
    private Integer stockQuantity;
    private String productStatus;
    private String searchKeywords;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 非持久化字段（方便业务层组装）
    private List<String> images;
    private List<String> mediaResources;

    // 关联的分类信息
    private Category category;

    // 文件：`backend/src/main/java/com/example/onlineshop/entity/Product.java`
    public String getImageUrl() {
        if (this.images == null || this.images.isEmpty()) {
            return null;
        }
        return this.images.get(0);
    }

    public void setImageUrl(String imageUrl) {
        if (this.images == null) {
            this.images = new java.util.ArrayList<>();
        }
        if (this.images.isEmpty()) {
            this.images.add(imageUrl);
        } else {
            this.images.set(0, imageUrl);
        }
    }

    // 手动添加getter和setter方法
    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public Integer getSellerId() {
        return sellerId;
    }

    public void setSellerId(Integer sellerId) {
        this.sellerId = sellerId;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductDesc() {
        return productDesc;
    }

    public void setProductDesc(String productDesc) {
        this.productDesc = productDesc;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
        // 当库存为0时，自动将商品状态设置为outOfStock
        if (stockQuantity != null && stockQuantity <= 0 && !"sold".equals(this.productStatus)) {
            this.productStatus = "outOfStock";
        }
    }

    public String getProductStatus() {
        return productStatus;
    }

    public void setProductStatus(String productStatus) {
        this.productStatus = productStatus;
    }

    public String getSearchKeywords() {
        return searchKeywords;
    }

    public void setSearchKeywords(String searchKeywords) {
        this.searchKeywords = searchKeywords;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public List<String> getMediaResources() {
        return mediaResources;
    }

    public void setMediaResources(List<String> mediaResources) {
        this.mediaResources = mediaResources;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }
}
