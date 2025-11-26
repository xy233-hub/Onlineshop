package com.example.onlineshop.dto.response;

import java.time.LocalDateTime;

public class FavoriteResponse {
    private Integer favoriteId;
    private Integer customerId;
    private Integer productId;
    private ProductInfoDto productInfo;
    private LocalDateTime favoritedAt;

    // getters / setters
    public Integer getFavoriteId() { return favoriteId; }
    public void setFavoriteId(Integer favoriteId) { this.favoriteId = favoriteId; }
    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }
    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }
    public ProductInfoDto getProductInfo() { return productInfo; }
    public void setProductInfo(ProductInfoDto productInfo) { this.productInfo = productInfo; }
    public LocalDateTime getFavoritedAt() { return favoritedAt; }
    public void setFavoritedAt(LocalDateTime favoritedAt) { this.favoritedAt = favoritedAt; }
}
