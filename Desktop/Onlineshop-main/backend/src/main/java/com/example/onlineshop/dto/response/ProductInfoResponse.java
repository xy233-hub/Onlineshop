// java
package com.example.onlineshop.dto.response;

import com.example.onlineshop.entity.Product;
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
    @JsonProperty("image_url")
    public String imageUrl; // 向后兼容：第一张图片
    @JsonProperty("images")
    public List<String> images; // 图片 URL 列表
    @JsonProperty("media_resources")
    public List<MediaResourceResponse> mediaResources;
    @JsonProperty("price")
    public BigDecimal price;
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

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ProductInfoResponse(Product p) {
        this(p, null, null);
    }

    public ProductInfoResponse(Product p, List<String> images, List<MediaResourceResponse> mediaResources) {
        if (p != null) {
            this.productId = p.getProductId();
            this.sellerId = p.getSellerId();
            this.productName = p.getProductName();
            this.productDesc = p.getProductDesc();
            this.price = p.getPrice();
            this.stockQuantity = p.getStockQuantity();
            this.productStatus = p.getProductStatus();
            this.searchKeywords = p.getSearchKeywords();
            this.createdAt = p.getCreatedAt() != null ? p.getCreatedAt().format(formatter) : null;
            this.updatedAt = p.getUpdatedAt() != null ? p.getUpdatedAt().format(formatter) : null;
        }

        this.images = images != null ? images : (p != null && p.getImages() != null ? p.getImages() : Collections.emptyList());

        if (!this.images.isEmpty()) {
            this.imageUrl = this.images.get(0);
        } else if (p != null && p.getCoverImage() != null && !p.getCoverImage().isEmpty()) {
            // 当 images 为空时，使用 Mapper 查询到的 coverImage 填充 image_url（优先展示）
            this.imageUrl = p.getCoverImage();
        } else {
            this.imageUrl = null;
        }

        this.mediaResources = mediaResources != null ? mediaResources : Collections.emptyList();
    }
}




