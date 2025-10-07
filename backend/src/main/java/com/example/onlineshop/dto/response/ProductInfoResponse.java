package com.example.onlineshop.dto.response;

import com.example.onlineshop.entity.Product;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

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
    public String imageUrl;
    @JsonProperty("price")
    public BigDecimal price;
    @JsonProperty("product_status")
    public String productStatus;
    @JsonProperty("created_at")
    public String createdAt;
    @JsonProperty("updated_at")
    public String updatedAt;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ProductInfoResponse(Product p) {
        this.productId = p.getProductId();
        this.sellerId = p.getSellerId();
        this.productName = p.getProductName();
        this.productDesc = p.getProductDesc();
        this.imageUrl = p.getImageUrl();
        this.price = p.getPrice();
        this.productStatus = p.getProductStatus();
        this.createdAt = p.getCreatedAt() != null ? p.getCreatedAt().format(formatter) : null;
        this.updatedAt = p.getUpdatedAt() != null ? p.getUpdatedAt().format(formatter) : null;
    }
}


