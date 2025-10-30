// file: backend/src/main/java/com/example/onlineshop/entity/ProductImage.java
package com.example.onlineshop.entity;

import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品图片（多张）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer imageId;
    private Integer productId;
    private String imageUrl;
    private Integer imageOrder; // 0 为封面
    private LocalDateTime createdAt;

    // 手动添加getter和setter方法
    public Integer getImageId() {
        return imageId;
    }

    public void setImageId(Integer imageId) {
        this.imageId = imageId;
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Integer getImageOrder() {
        return imageOrder;
    }

    public void setImageOrder(Integer imageOrder) {
        this.imageOrder = imageOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
