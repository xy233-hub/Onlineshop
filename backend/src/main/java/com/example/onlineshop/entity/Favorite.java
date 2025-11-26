package com.example.onlineshop.entity;

import java.time.LocalDateTime;

public class Favorite {
    private Integer favoriteId;
    private Integer customerId;
    private Integer productId;
    private LocalDateTime createdAt;

    // getters / setters
    public Integer getFavoriteId() { return favoriteId; }
    public void setFavoriteId(Integer favoriteId) { this.favoriteId = favoriteId; }
    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }
    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
