package com.example.onlineshop.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

public class ProductImageResponse {
    @JsonProperty("image_id")
    public Integer imageId;
    @JsonProperty("product_id")
    public Integer productId;
    @JsonProperty("image_url")
    public String imageUrl;
    @JsonProperty("image_order")
    public Integer imageOrder;
    @JsonProperty("created_at")
    public String createdAt;

    private static final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ProductImageResponse() {}

    public ProductImageResponse(Integer imageId, Integer productId, String imageUrl, Integer imageOrder, LocalDateTime createdAt) {
        this.imageId = imageId;
        this.productId = productId;
        this.imageUrl = imageUrl;
        this.imageOrder = imageOrder;
        this.createdAt = createdAt != null ? createdAt.format(fmt) : null;
    }
}
