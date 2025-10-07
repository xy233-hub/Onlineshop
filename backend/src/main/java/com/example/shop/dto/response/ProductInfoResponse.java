
package com.example.shop.dto.response;

import com.example.shop.entity.Product;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

public class ProductInfoResponse {
    public Integer productId;
    public Integer sellerId;
    public String productName;
    public String productDesc;
    public String imageUrl;
    public BigDecimal price;
    public String productStatus;
    public String createdAt;
    public String updatedAt;

    // 添加静态时间格式化器
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

