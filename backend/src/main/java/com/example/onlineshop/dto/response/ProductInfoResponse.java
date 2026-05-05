// java
package com.example.onlineshop.dto.response;

import com.example.onlineshop.entity.Product;
import com.example.onlineshop.service.PromotionPriceService;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

public class ProductInfoResponse {
    @JsonProperty("product_id")
    public Integer productId;
    @JsonProperty("seller_id")
    public Integer sellerId;
    @JsonProperty("product_name")
    public String productName;
    @JsonProperty("product_desc")
    public String productDesc;
    @JsonProperty("short_desc")
    public String shortDesc;
    @JsonProperty("image_url")
    public String imageUrl;
    @JsonProperty("images")
    public List<String> images;
    @JsonProperty("media_resources")
    public List<MediaResourceResponse> mediaResources;
    @JsonProperty("price")
    public BigDecimal price;
    @JsonProperty("original_price")
    public BigDecimal originalPrice;
    @JsonProperty("current_promotion_price")
    public BigDecimal currentPromotionPrice;
    @JsonProperty("has_active_promotion")
    public Boolean hasActivePromotion;
    @JsonProperty("stock_quantity")
    public Integer stockQuantity;
    @JsonProperty("product_status")
    public String productStatus;
    @JsonProperty("search_keywords")
    public String searchKeywords;
    @JsonProperty("created_at")
    public String createdAt;
    @JsonProperty("updated_at")
    public String updatedAt;
    @JsonProperty("score")
    public Double score;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ProductInfoResponse(Product p) {
        this(p, null, null, null);
    }

    public ProductInfoResponse(Product p, List<String> images, List<MediaResourceResponse> mediaResources) {
        this(p, images, mediaResources, null);
    }

    public ProductInfoResponse(Product p, List<String> images, List<MediaResourceResponse> mediaResources, PromotionPriceService.PromotionPriceResult promoResult) {
        if (p != null) {
            this.productId = p.getProductId();
            this.sellerId = p.getSellerId();
            this.productName = p.getProductName();
            this.productDesc = p.getProductDesc();
            this.shortDesc = p.getShortDesc();
            this.price = p.getPrice();
            this.stockQuantity = p.getStockQuantity();
            this.productStatus = p.getProductStatus();
            this.searchKeywords = p.getSearchKeywords();
            this.createdAt = p.getCreatedAt() != null ? p.getCreatedAt().format(formatter) : null;
            this.updatedAt = p.getUpdatedAt() != null ? p.getUpdatedAt().format(formatter) : null;

            if (promoResult != null) {
                this.originalPrice = promoResult.originalPrice;
                this.currentPromotionPrice = promoResult.currentPromotionPrice;
                this.hasActivePromotion = promoResult.hasActivePromotion;
            } else {
                this.originalPrice = p.getOriginalPrice();
                this.currentPromotionPrice = p.getCurrentPromotionPrice();
                this.hasActivePromotion = p.getHasActivePromotion();
            }
        }

        this.images = images != null ? images : (p != null && p.getImages() != null ? p.getImages() : Collections.emptyList());

        if (!this.images.isEmpty()) {
            this.imageUrl = this.images.get(0);
        } else if (p != null && p.getCoverImage() != null && !p.getCoverImage().isEmpty()) {
            this.imageUrl = p.getCoverImage();
        } else {
            this.imageUrl = null;
        }

        this.mediaResources = mediaResources != null ? mediaResources : Collections.emptyList();
    }
}
